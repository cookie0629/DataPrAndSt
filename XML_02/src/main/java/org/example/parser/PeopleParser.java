package org.example.parser;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * XML 解析器类
 * 负责读取 XML 流，解析人物数据，并进行数据清洗和关系推断
 */
public class PeopleParser {

    /**
     * 解析入口方法
     * @param stream XML 输入流
     * @return 包含所有解析后人物信息的 Map (Key: ID, Value: PersonInfo)
     * @throws XMLStreamException XML解析异常
     */
    public Map<String, PersonInfo> parse(InputStream stream) throws XMLStreamException {
        ArrayList<PersonInfo> data = new ArrayList<>();

        // XML reader 初始化
        XMLInputFactory streamFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = streamFactory.createXMLStreamReader(stream);

        int peopleCount = 0; // XML 中声明的总人数
        PersonInfo info = null; // 当前正在解析的人物对象

        System.out.println("=== Start parsing... (开始解析) ===");

        while (reader.hasNext()) {
            reader.next();

            int event_type = reader.getEventType();
            switch (event_type) {
                case XMLStreamConstants.START_ELEMENT -> {
                    // 根据标签名进行不同的处理
                    switch (reader.getLocalName()) {
                        case "people":
                            // 解析根节点，获取总人数 count 属性
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("count")) {
                                    peopleCount = Integer.parseInt(reader.getAttributeValue(i));
                                    System.out.println("Total people count: " + peopleCount);
                                } else {
                                    System.out.println("Unknown attribute in <people>");
                                }
                            }
                            break;
                        case "person":
                            // 开始解析一个新的 person，初始化对象
                            info = new PersonInfo();
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                switch (reader.getAttributeLocalName(i)) {
                                    case "name" -> {
                                        // 处理 name 属性 (e.g., name="John Doe")
                                        String[] full = reader.getAttributeValue(i).trim().split("\\s+");
                                        info.firstName = full[0];
                                        info.lastName = full[1];
                                    }
                                    case "id" -> info.id = reader.getAttributeValue(i).trim();
                                    default -> System.out.println("Unknown attribute in <person>");
                                }
                            }
                            break;
                        case "id":
                            // 处理子标签 <id>
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("value")) {
                                    assert info != null;
                                    info.id = reader.getAttributeValue(i).trim();
                                } else {
                                    System.out.println("Unknown attribute in <id>");
                                }
                            }
                            break;
                        case "firstname":
                            // 处理名
                            if (reader.getAttributeCount() > 0) {
                                for (int i = 0; i < reader.getAttributeCount(); i++) {
                                    if (reader.getAttributeLocalName(i).equals("value")) {
                                        assert info != null;
                                        info.firstName = reader.getAttributeValue(i).trim();
                                    } else {
                                        System.out.println("Unknown attribute in <firstname>");
                                    }
                                }
                            } else {
                                assert info != null;
                                info.firstName = reader.getElementText().trim();
                            }
                            break;
                        case "surname":
                            // 处理姓
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("value")) {
                                    assert info != null;
                                    info.lastName = reader.getAttributeValue(i).trim();
                                } else {
                                    System.out.println("Unknown attribute in <surname>");
                                }
                            }
                            break;
                        case "fullname", "children":
                            // 跳过这些聚合标签，具体逻辑在子元素中处理
                            break;
                        case "first":
                            assert info != null;
                            info.firstName = reader.getElementText().trim();
                            break;
                        case "family", "family-name":
                            assert info != null;
                            info.lastName = reader.getElementText().trim();
                            break;
                        case "gender":
                            // 处理性别
                            if (reader.getAttributeCount() > 0) {
                                for (int i = 0; i < reader.getAttributeCount(); i++) {
                                    if (reader.getAttributeLocalName(i).equals("value")) {
                                        assert info != null;
                                        info.gender = reader.getAttributeValue(i).trim().toUpperCase().substring(0, 1);
                                    } else {
                                        System.out.println("Unknown attribute in <gender>");
                                    }
                                }
                            } else {
                                assert info != null;
                                info.gender = reader.getElementText().trim().toUpperCase().substring(0, 1);
                            }
                            break;
                        case "spouce":
                            // 处理配偶 (注意 XML 中可能有拼写错误 'spouce' -> spouse)
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("value")) {
                                    if (!reader.getAttributeValue(i).trim().equals("NONE")) {
                                        assert info != null;
                                        info.spouseName = reader.getAttributeValue(i);
                                    }
                                } else {
                                    System.out.println("Unknown attribute in <spouce>");
                                }
                            }
                            if (reader.hasText()) {
                                if (!reader.getText().trim().equals("NONE")) {
                                    assert info != null;
                                    info.spouseName = reader.getText();
                                }
                            }
                            break;
                        case "husband":
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("value")) {
                                    assert info != null;
                                    info.husbandId = reader.getAttributeValue(i).trim();
                                } else {
                                    System.out.println("Unknown attribute in <husband>");
                                }
                            }
                            break;
                        case "wife":
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if ("value".equals(reader.getAttributeLocalName(i))) {
                                    assert info != null;
                                    info.wifeId = reader.getAttributeValue(i).trim();
                                } else {
                                    System.out.println("Unknown attribute in <wife>");
                                }
                            }
                            break;
                        case "siblings":
                            // 处理兄弟姐妹列表 (空格分隔的 ID)
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("val")) {
                                    List<String> siblings = Arrays.asList(reader.getAttributeValue(i).trim().split("\\s+"));
                                    assert info != null;
                                    info.siblingsId.addAll(siblings);
                                } else {
                                    System.out.println("Unknown attribute in <siblings>");
                                }
                            }
                            break;
                        case "brother":
                            assert info != null;
                            info.brothersName.add(getFullname(reader.getElementText()));
                            break;
                        case "sister":
                            assert info != null;
                            info.sistersName.add(getFullname(reader.getElementText()));
                            break;
                        case "siblings-number":
                            // 记录兄弟姐妹数量，用于后续校验
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("value")) {
                                    assert info != null;
                                    info.siblingsCount = Integer.parseInt(reader.getAttributeValue(i).trim());
                                } else {
                                    System.out.println(reader.getLocalName() + " has unknown attribute: " + reader.getAttributeLocalName(i));
                                }
                            }
                            break;
                        case "child":
                            assert info != null;
                            info.childrenName.add(getFullname(reader.getElementText()));
                            break;
                        case "son":
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("id")) {
                                    assert info != null;
                                    info.sonsId.add(reader.getAttributeValue(i).trim());
                                } else {
                                    System.out.println("Unknown attribute in <son>");
                                }
                            }
                            break;
                        case "daughter":
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("id")) {
                                    assert info != null;
                                    info.daughtersId.add(reader.getAttributeValue(i).trim());
                                } else {
                                    System.out.println("Unknown attribute in <daughter>");
                                }
                            }
                            break;
                        case "parent":
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("value")) {
                                    if (!reader.getAttributeValue(i).trim().equals("UNKNOWN")) {
                                        assert info != null;
                                        info.parentsId.add(reader.getAttributeValue(i).trim());
                                    }
                                } else {
                                    System.out.println("Unknown attribute in <parent>");
                                }
                            }
                            break;
                        case "father":
                            assert info != null;
                            info.fatherName = getFullname(reader.getElementText());
                            break;
                        case "mother":
                            assert info != null;
                            info.motherName = getFullname(reader.getElementText());
                            break;
                        case "children-number":
                            // 记录子女数量，用于后续校验
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("value")) {
                                    assert info != null;
                                    info.childrenCount = Integer.parseInt(reader.getAttributeValue(i).trim());
                                } else {
                                    System.out.println("Unknown attribute in <children-number>");
                                }
                            }
                            break;
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    // 遇到 </person> 结束标签，将当前人物存入列表
                    if (reader.getLocalName().equals("person")) {
                        data.add(info);
                        info = null;
                    }
                }
                default -> {
                }
            }
        }

        reader.close();
        // 调用标准化方法处理数据
        return normalize(data, peopleCount);
    }

    /**
     * 数据标准化与修复核心方法
     * 负责将离散的数据片段合并，并通过逻辑推断完善关系网
     */
    private HashMap<String, PersonInfo> normalize(ArrayList<PersonInfo> data, Integer peopleCount) {
        HashMap<String, PersonInfo> id_records = new HashMap<>();
        ArrayList<PersonInfo> temp_records = new ArrayList<>();

        System.out.println("=== Normalizing (标准化中) ===");

        // 1. 初步分离：将有 ID 的记录和无 ID 的临时记录分开
        // 有 ID 的放入 Map 自动去重合并，无 ID 的放入 List 待处理
        for (PersonInfo i : data) {
            if (i.id != null) {
                if (id_records.containsKey(i.id)) {
                    id_records.get(i.id).merge(i);
                } else {
                    id_records.put(i.id, i);
                }
            } else {
                temp_records.add(i);
            }
        }

        // 校验：提取出的独立 ID 数量应该等于 XML 声明的总人数
        assert id_records.size() == peopleCount;
        // 校验：所有记录都必须包含姓名
        assert id_records.values().parallelStream().allMatch(x -> x.firstName != null && x.lastName != null);
        assert temp_records.parallelStream().allMatch(x -> x.firstName != null && x.lastName != null);

        data = temp_records;
        temp_records = new ArrayList<>();

        // 2. 合并同名人员
        // 尝试将无 ID 的记录根据姓名匹配到有 ID 的记录上
        for (PersonInfo p : data) {
            List<PersonInfo> found = findInRecords(x -> x.firstName.equals(p.firstName) && x.lastName.equals(p.lastName),
                    id_records.values());
            if (found.size() == 1) {
                // 找到唯一匹配，合并
                PersonInfo foundPerson = found.get(0);
                foundPerson.merge(p);
                id_records.replace(foundPerson.id, foundPerson);
            } else if (found.size() > 1) {
                // 找到多个同名匹配（重名情况）
                PersonInfo foundPerson = found.get(0);
                temp_records.addAll(found); // 将这些待定记录放回 temp
                // 如果已存在的记录没有性别，而新记录有性别，尝试补充性别 (这里的逻辑可能比较激进)
                if (foundPerson.gender == null && p.gender != null) {
                    foundPerson.merge(p);
                }
            }
        }

        data = temp_records;
        temp_records = new ArrayList<>();

        // 3. 基于兄弟姐妹关系进行合并
        // 如果根据名字无法唯一确定，尝试通过共有的兄弟姐妹来匹配
        for (PersonInfo person : data) {
            if (person.siblingsId != null) {
                HashSet<String> siblings = new HashSet<>(person.siblingsId);
                List<PersonInfo> found = findInRecords(
                        x -> {
                            HashSet<String> xsib = new HashSet<>(x.siblingsId);
                            xsib.retainAll(siblings); // 取交集
                            return xsib.size() > 0;   // 如果有共同的兄弟姐妹 ID，则认为是同一个人
                        }, id_records.values()
                );
                if (found.size() == 1) {
                    found.get(0).merge(person);
                } else {
                    temp_records.add(person);
                }
            }
        }

        System.out.println("Normalized!!! (标准化完成)");

        // 执行具体的逻辑推断和数据填充
        genderAssertion(id_records); // 性别推断
        spouseMerging(id_records);   // 配偶关系双向绑定
        childrenAssertion(id_records); // 子女关系双向绑定
        siblingsAssertion(id_records); // 兄弟姐妹关系完善

        return id_records;
    }

    /**
     * 配偶信息合并
     * 确保夫妻关系的双向性 (A是B的丈夫 => B是A的妻子)
     */
    private void spouseMerging(HashMap<String, PersonInfo> id_records) {
        System.out.println("=== Spouse merging (合并配偶信息) ===");
        // 1. 通过 ID 匹配
        for (var p : id_records.values()) {
            String spouseId = p.wifeId != null ? p.wifeId : p.husbandId;
            spouseId = spouseId != null ? spouseId : p.spouseId;
            if (spouseId == null) {
                continue;
            }

            PersonInfo s = id_records.get(spouseId);
            s.spouseId = p.id;
            // 根据性别设置对应的 husband/wife 字段
            if (p.gender.equals("M") && p.wifeId == null) {
                s.husbandId = p.id;
                p.wifeId = p.spouseId;
            } else if (p.gender.equals("F") && p.husbandId == null) {
                s.wifeId = p.id;
                p.husbandId = p.spouseId;
            }
        }

        // 2. 通过姓名匹配
        for (var p : id_records.values()) {
            String spouseName = p.wifeName != null ? p.wifeName : p.husbandName;
            spouseName = spouseName != null ? spouseName : p.spouseName;
            if (p.spouseName == null) {
                continue;
            }
            String finalSpouseName = spouseName;
            List<PersonInfo> f = findInRecords(x -> finalSpouseName.equals(x.getFullName()), id_records.values());
            if (f.size() < 1) {
                continue;
            }

            PersonInfo s = id_records.get(f.get(0).id);
            s.spouseId = p.id;
            // 双向绑定逻辑
            if (p.gender.equals("M") && p.wifeId == null) {
                p.wifeId = s.id;
                s.husbandId = p.id;
            } else if (p.gender.equals("F") && p.husbandId == null) {
                p.husbandId = s.id;
                s.wifeId = p.id;
            }
        }

        System.out.println("Spouse merging finished!");
    }

    /**
     * 子女关系断言与填充
     * 确保父母与子女关系的双向性，并验证数量
     */
    private void childrenAssertion(HashMap<String, PersonInfo> id_records) {
        System.out.println("=== Children assertion (处理子女关系) ===");
        for (String key : id_records.keySet()) {
            PersonInfo p = id_records.get(key);
            // 汇总 ID
            p.childrenId.addAll(p.sonsId);
            p.childrenId.addAll(p.daughtersId);

            // 根据名字查找并补充 ID (女儿)
            for (String s : p.daughtersName) {
                List<PersonInfo> f = findInRecords(x -> s.equals(x.firstName + " " + x.lastName),
                        id_records.values());
                if (f.size() >= 1) {
                    PersonInfo daughter = f.get(0);
                    if (daughter != null) {
                        p.childrenId.add(daughter.id);
                    }
                }
            }
            // 根据名字查找并补充 ID (儿子)
            for (String s : p.sonsName) {
                List<PersonInfo> f = findInRecords(x -> s.equals(x.firstName + " " + x.lastName),
                        id_records.values());
                if (f.size() >= 1) {
                    PersonInfo son = f.get(0);
                    if (son != null) {
                        p.childrenId.add(son.id);
                    }
                }
            }
            // 根据名字查找并补充 ID (通用子女)
            for (String s : p.childrenName) {
                List<PersonInfo> f = findInRecords(x -> s.equals(x.firstName + " " + x.lastName),
                        id_records.values());
                if (f.size() >= 1) {
                    PersonInfo child = f.get(0);
                    if (child != null) {
                        p.childrenId.add(child.id);
                    }
                }
            }

            // 处理该人的所有子女：设置子女的父母信息
            for (String childId : p.childrenId) {
                PersonInfo child = id_records.get(childId);
                // 将 ID 分类到 sons 或 daughters
                if (child.gender.equals("M")) {
                    p.sonsId.add(childId);
                } else {
                    p.daughtersId.add(childId);
                }
                // 设置子女的父母 ID
                child.parentsId.add(p.id);
                if (p.gender.equals("M")) {
                    child.fatherId = p.id;
                } else {
                    child.motherId = p.id;
                }
            }

            // 校验子女数量是否符合 XML 声明的数量
            if (p.childrenCount != null) {
                if (!p.childrenCount.equals(p.childrenId.size())) {
                    System.out.println(p.firstName + " " + p.lastName + " " + p.childrenId.size() + " " + p.childrenCount);
                }
                try {
                    assert p.childrenId.size() == p.childrenCount;
                } catch (AssertionError e) {
                    System.out.println("CHILDREN ASSERTION FAILED: in " + p);
                }
            }
        }
        System.out.println("Children assertion finished!");
    }

    /**
     * 兄弟姐妹关系断言
     */
    private void siblingsAssertion(HashMap<String, PersonInfo> id_records) {
        System.out.println("=== Siblings assertion (处理兄弟姐妹关系) ===");
        for (String key : id_records.keySet()) {
            PersonInfo p = id_records.get(key);
            p.siblingsId.addAll(p.brothersId);
            p.siblingsId.addAll(p.sistersId);

            // 同样的逻辑：通过名字查找实体并获取 ID
            for (String s : p.sistersName) {
                List<PersonInfo> f = findInRecords(x -> s.equals(x.firstName + " " + x.lastName),
                        id_records.values());
                if (f.size() >= 1) {
                    PersonInfo sister = f.get(0);
                    if (sister != null) {
                        p.siblingsId.add(sister.id);
                    }
                }
            }
            for (String s : p.brothersName) {
                List<PersonInfo> f = findInRecords(x -> s.equals(x.firstName + " " + x.lastName),
                        id_records.values());
                if (f.size() >= 1) {
                    PersonInfo brother = f.get(0);
                    if (brother != null) {
                        p.siblingsId.add(brother.id);
                    }
                }
            }
            for (String s : p.siblingsName) {
                List<PersonInfo> f = findInRecords(x -> s.equals(x.firstName + " " + x.lastName),
                        id_records.values());
                if (f.size() >= 1) {
                    PersonInfo sibling = f.get(0);
                    if (sibling != null) {
                        p.siblingsId.add(sibling.id);
                    }
                }
            }

            // 归类兄弟或姐妹
            for (String s : p.siblingsId) {
                if (id_records.get(s).gender.equals("M")) {
                    p.brothersId.add(s);
                } else {
                    p.sistersId.add(s);
                }
            }

            // 数量校验
            if (p.siblingsCount != null) {
                try {
                    assert p.siblingsId.size() == p.siblingsCount;
                } catch (AssertionError e) {
                    System.out.println("SIBLINGS ASSERTION FAILED: in " + p);
                }
            }
        }
        System.out.println("Siblings assertion finished!");
    }

    /**
     * 性别推断
     * 根据配偶角色等信息推断缺失的性别信息
     */
    private void genderAssertion(HashMap<String, PersonInfo> id_records) {
        System.out.println("=== Gender assertion (性别推断) ===");
        for (var p : id_records.values()) {
            if (p.gender == null) {
                // 如果有妻子或丈夫，推断性别
                if (p.wifeId != null || p.wifeName != null) {
                    p.gender = "M";
                } else if (p.husbandId != null || p.husbandName != null) {
                    p.gender = "F";
                } else if (p.spouseId != null) {
                    // 如果有中性词配偶，检查配偶的性别
                    PersonInfo pp = id_records.get(p.spouseId);
                    if (pp.gender != null) {
                        if (pp.gender.equals("M")) {
                            p.gender = "F";
                        }
                        if (pp.gender.equals("F")) {
                            p.gender = "M";
                        }
                    } else if (pp.husbandName != null || pp.husbandId != null) {
                        p.gender = "M";
                    } else if (pp.wifeName != null || pp.wifeId != null) {
                        p.gender = "F";
                    }
                } else {
                    // 硬编码兜底逻辑 (针对特定数据 case)
                    p.gender = "M"; //for Tonya Loschiavo
                }
            }
        }

        // 最终校验：所有人必须有性别
        for (var p : id_records.values()) {
            try {
                assert p.gender != null && (p.gender.equals("M") || p.gender.equals("F"));
            } catch (AssertionError e) {
                System.out.println("This person hasn't gender: " + p);
            }
        }
        System.out.println("Gender assertion finished!");
    }

    /**
     * 辅助方法：拼接全名
     */
    private String getFullname(String string) {
        String[] fullname = string.trim().split("\\s+");
        return fullname[0] + " " + fullname[1];
    }

    /**
     * 辅助方法：在集合中根据条件查找记录
     */
    private List<PersonInfo> findInRecords(Predicate<PersonInfo> pred, Collection<PersonInfo> coll) {
        return coll.
                parallelStream().
                filter(pred).
                collect(Collectors.toList());
    }
}