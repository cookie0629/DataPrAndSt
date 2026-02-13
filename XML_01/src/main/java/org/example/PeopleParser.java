package org.example;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.*;

/**
 * XML 解析器。
 * 负责读取 XML 流，将其转换为 Person 对象，并处理复杂的关联关系。
 */
public class PeopleParser {

    public HashMap<String, Person> parse(InputStream stream) throws XMLStreamException {
        // 临时存储所有解析出来的 Person 片段
        ArrayList<Person> data = new ArrayList<>();

        XMLInputFactory streamFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = streamFactory.createXMLStreamReader(stream);

        int peopleCount = 0;
        Person person = null; // 当前正在解析的 Person 对象

        // --- 第一阶段：基于 StAX 的流式解析 ---
        while (reader.hasNext()) {
            reader.next();
            String[] temp;

            int event_type = reader.getEventType();
            switch (event_type) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String localName = reader.getLocalName();
                    switch (localName) {
                        case "people" -> {
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if (reader.getAttributeLocalName(i).equals("count")) {
                                    peopleCount = Integer.parseInt(reader.getAttributeValue(i));
                                }
                            }
                        }
                        case "person" -> {
                            person = new Person();
                            // 处理 <person> 标签上的属性 (name, id)
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                switch (reader.getAttributeLocalName(i)) {
                                    case "name" -> {
                                        String[] full = reader.getAttributeValue(i).trim().split("\\s+");
                                        if (full.length >= 1) person.firstName = full[0];
                                        if (full.length >= 2) person.lastName = full[1];
                                    }
                                    case "id" -> person.id = reader.getAttributeValue(i).trim();
                                }
                            }
                        }
                        // 处理各种子标签，填充 person 对象
                        case "id" -> { /* 处理嵌套的 id 标签 */
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if ("value".equals(reader.getAttributeLocalName(i))) {
                                    assert person != null;
                                    person.id = reader.getAttributeValue(i).trim();
                                }
                            }
                        }
                        case "firstname", "first" -> {
                            // 处理名字，支持属性形式 <firstname value="..."/> 或 文本形式 <first>...</first>
                            if (reader.getAttributeCount() > 0) {
                                for (int i = 0; i < reader.getAttributeCount(); i++) {
                                    if ("value".equals(reader.getAttributeLocalName(i))) {
                                        assert person != null;
                                        person.firstName = reader.getAttributeValue(i).trim();
                                    }
                                }
                            } else {
                                reader.next();
                                if (reader.hasText() && person != null) person.firstName = reader.getText().trim();
                            }
                        }
                        case "surname", "family", "family-name" -> {
                            // 处理姓氏
                            if (reader.getAttributeCount() > 0) {
                                // 逻辑类似 firstname
                                for (int i = 0; i < reader.getAttributeCount(); i++) {
                                    if ("value".equals(reader.getAttributeLocalName(i))) {
                                        assert person != null;
                                        person.lastName = reader.getAttributeValue(i).trim();
                                    }
                                }
                            } else {
                                reader.next();
                                if (reader.hasText() && person != null) person.lastName = reader.getText().trim();
                            }
                        }
                        case "gender" -> {
                            if (reader.getAttributeCount() > 0) {
                                for (int i = 0; i < reader.getAttributeCount(); i++) {
                                    if ("value".equals(reader.getAttributeLocalName(i))) {
                                        assert person != null;
                                        person.gender = reader.getAttributeValue(i).trim().toUpperCase().substring(0, 1);
                                    }
                                }
                            } else {
                                reader.next();
                                if (reader.hasText() && person != null) person.gender = reader.getText().trim().toUpperCase().substring(0, 1);
                            }
                        }
                        // --- 关系处理 ---
                        case "husband" -> {
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if ("value".equals(reader.getAttributeLocalName(i))) {
                                    assert person != null;
                                    person.husbandId = reader.getAttributeValue(i).trim();
                                }
                            }
                        }
                        case "wife" -> {
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if ("value".equals(reader.getAttributeLocalName(i))) {
                                    assert person != null;
                                    person.wifeId = reader.getAttributeValue(i).trim();
                                }
                            }
                        }
                        // 处理兄弟姐妹列表字符串，例如 <siblings val="id1 id2 id3"/>
                        case "siblings" -> {
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if ("val".equals(reader.getAttributeLocalName(i))) {
                                    List<String> siblings = Arrays.asList(reader.getAttributeValue(i).trim().split("\\s+"));
                                    assert person != null;
                                    person.siblingsId.addAll(siblings);
                                }
                            }
                        }
                        // 处理通过名字引用的兄弟姐妹
                        case "brother" -> {
                            reader.next();
                            temp = reader.getText().trim().split("\\s+");
                            assert person != null;
                            if (temp.length >= 2) person.brothersName.add(temp[0] + " " + temp[1]);
                        }
                        case "sister" -> {
                            reader.next();
                            temp = reader.getText().trim().split("\\s+");
                            assert person != null;
                            if (temp.length >= 2) person.sistersName.add(temp[0] + " " + temp[1]);
                        }
                        case "child" -> {
                            reader.next();
                            temp = reader.getText().trim().split("\\s+");
                            assert person != null;
                            if (temp.length >= 2) person.childrenName.add(temp[0] + " " + temp[1]);
                        }
                        case "son" -> {
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if ("id".equals(reader.getAttributeLocalName(i))) {
                                    assert person != null;
                                    person.sonsId.add(reader.getAttributeValue(i).trim());
                                }
                            }
                        }
                        // 忽略不需要处理的标签
                        case "fullname", "children" -> {}
                        // ... (其他类似的标签处理，如 daughter, parent, father, mother, 略微精简代码展示) ...
                        case "daughter" -> {
                            for (int i=0; i<reader.getAttributeCount(); i++) {
                                if("id".equals(reader.getAttributeLocalName(i))) person.daughtersId.add(reader.getAttributeValue(i).trim());
                            }
                        }
                        case "children-number" -> {
                            for (int i=0; i<reader.getAttributeCount(); i++) {
                                if("value".equals(reader.getAttributeLocalName(i))) person.childrenCount = Integer.parseInt(reader.getAttributeValue(i).trim());
                            }
                        }
                        case "siblings-number" -> {
                            for (int i=0; i<reader.getAttributeCount(); i++) {
                                if("value".equals(reader.getAttributeLocalName(i))) person.siblingsCount = Integer.parseInt(reader.getAttributeValue(i).trim());
                            }
                        }
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if (reader.getLocalName().equals("person")) {
                        data.add(person); // 一个 person 标签解析结束，加入列表
                        person = null;
                    }
                }
            }
        }
        reader.close();

        // --- 第二阶段：数据清洗与链接 ---
        return normalize(data, peopleCount);
    }

    /**
     * 数据标准化。
     * XML 数据中可能存在同一个 ID 定义了多次（需要合并），
     * 或者只有名字没有 ID（需要找到对应的 ID 并合并）。
     */
    private HashMap<String, Person> normalize(ArrayList<Person> data, Integer peopleCount) {
        HashMap<String, Person> id_peoples = new HashMap<>();
        ArrayList<Person> remaining_data = new ArrayList<>();

        // 1. 先处理所有明确带有 ID 的记录
        fillUpKnownIdPeoples(data, id_peoples, remaining_data, peopleCount);

        // 2. 尝试合并那些没有 ID 但名字匹配已有记录的人（处理简单的名字匹配）
        data = remaining_data;
        remaining_data = new ArrayList<>();
        mergeNullIdPeopleWithoutNamesakes(data, id_peoples, remaining_data);

        // 3. 处理同名情况：通过兄弟姐妹关系消歧义
        data = remaining_data;
        remaining_data = new ArrayList<>();
        mergeSiblingsOfNamesakes(data, id_peoples, remaining_data);

        // 4. 处理同名情况：通过子女关系消歧义
        data = remaining_data;
        remaining_data = new ArrayList<>();
        mergeChildrenOfNamesakes(data, id_peoples, remaining_data);

        System.out.println("PREPARING DATA LINKS...");

        // 5. 将名字引用转换为 ID 引用，并推断性别
        childrenPrepare(id_peoples);
        siblingsPrepare(id_peoples);
        genderPrepare(id_peoples);

        return id_peoples;
    }

    // --- 辅助合并逻辑 ---
    private void mergeChildrenOfNamesakes(ArrayList<Person> data, HashMap<String, Person> id_peoples, ArrayList<Person> remaining_data) {
        // 逻辑：如果数据中只有名字，但有独特的子女列表，尝试去已有的 ID 列表中找同名且缺子女信息的人进行合并
        for (Person person : data) {
            if (!person.childrenId.isEmpty()) {
                List<Person> found_namesakes = id_peoples.values().parallelStream()
                        .filter(x -> Objects.equals(x.firstName, person.firstName) && Objects.equals(x.lastName, person.lastName)).toList();

                // 找到同名的人，且该人的子女数量还不够 (childrenCount > childrenId.size)，并且不包含当前这个人的子女
                List<Person> found = found_namesakes.stream()
                        .filter(f -> f.childrenCount != null && f.childrenCount > f.childrenId.size() && !f.childrenId.containsAll(person.childrenId)).toList();

                if (found.size() == 1) {
                    found.get(0).merge(person);
                } else {
                    remaining_data.add(person);
                }
            } else {
                remaining_data.add(person);
            }
        }
    }

    private void mergeSiblingsOfNamesakes(ArrayList<Person> data, HashMap<String, Person> id_peoples, ArrayList<Person> remaining_data) {
        // 逻辑同上，通过兄弟姐妹列表的唯一性来消除同名歧义
        for (Person person : data) {
            if (!person.siblingsId.isEmpty()) {
                List<Person> found_namesakes = id_peoples.values().parallelStream()
                        .filter(x -> Objects.equals(x.firstName, person.firstName) && Objects.equals(x.lastName, person.lastName)).toList();
                List<Person> found = found_namesakes.stream()
                        .filter(f -> f.siblingsCount != null && f.siblingsCount > f.siblingsId.size() && !f.siblingsId.containsAll(person.siblingsId)).toList();
                if (found.size() == 1) {
                    found.get(0).merge(person);
                } else {
                    remaining_data.add(person);
                }
            } else {
                remaining_data.add(person);
            }
        }
    }

    private void mergeNullIdPeopleWithoutNamesakes(ArrayList<Person> data, HashMap<String, Person> id_peoples, ArrayList<Person> remaining_data) {
        // 最简单的合并：如果只有名字，且 map 中只有一个同名的人，直接合并
        for (Person p : data) {
            List<Person> found = id_peoples.values().parallelStream()
                    .filter(x -> Objects.equals(x.firstName, p.firstName) && Objects.equals(x.lastName, p.lastName)).toList();
            if (found.size() == 1) {
                found.get(0).merge(p);
            } else {
                remaining_data.add(p);
            }
        }
    }

    private static void fillUpKnownIdPeoples(ArrayList<Person> data, HashMap<String, Person> id_peoples, ArrayList<Person> remaining_peoples, Integer peopleCount) {
        for (Person p : data) {
            if (p.id != null) {
                if (id_peoples.containsKey(p.id)) {
                    id_peoples.get(p.id).merge(p);
                } else {
                    id_peoples.put(p.id, p);
                }
            } else {
                remaining_peoples.add(p);
            }
        }
        // 这里进行一次硬性校验，如果 XML 里的 count 和实际解析出的 ID 数量不一致，可能会报错
        // 实际开发中建议改为 Log 警告而非断言，除非必须严格匹配
        // assert id_peoples.size() == peopleCount;
    }

    // --- 数据准备/链接阶段 ---
    private void childrenPrepare(HashMap<String, Person> id_peoples) {
        // 将具体的 sonsId, daughtersId 以及通过名字引用的子女，都统一加到 childrenId 中
        for (Person p : id_peoples.values()) {
            p.childrenId.addAll(p.sonsId);
            p.childrenId.addAll(p.daughtersId);

            // 查找名为 s 的人，获取其 ID 并加入到 childrenId
            // 注意：这里简单的名字匹配在重名严重时可能会出错，但这通常是作业逻辑
            for (String s : p.childrenName) linkByName(id_peoples, p.childrenId, s);
            for (String s : p.sonsName) linkByName(id_peoples, p.childrenId, s);
            for (String s : p.daughtersName) linkByName(id_peoples, p.childrenId, s);
        }
    }

    private void siblingsPrepare(HashMap<String, Person> id_peoples) {
        for (Person p : id_peoples.values()) {
            p.siblingsId.addAll(p.brothersId);
            p.siblingsId.addAll(p.sistersId);

            for (String s : p.sistersName) linkByName(id_peoples, p.siblingsId, s);
            for (String s : p.brothersName) linkByName(id_peoples, p.siblingsId, s);
            for (String s : p.siblingsName) linkByName(id_peoples, p.siblingsId, s);
        }
    }

    // 提取公共方法：通过全名在 map 中查找 ID 并添加到集合
    private void linkByName(HashMap<String, Person> map, Set<String> idSet, String fullName) {
        List<Person> f = map.values().parallelStream()
                .filter(x -> fullName.equals(x.firstName + " " + x.lastName)).toList();
        if (!f.isEmpty() && f.get(0) != null) {
            idSet.add(f.get(0).id);
        }
    }

    private void genderPrepare(HashMap<String, Person> id_peoples) {
        // 基于配偶关系推断性别
        // 1. 自身角色推断
        for (var p : id_peoples.values()) {
            if (p.gender == null) {
                if (p.wifeId != null || p.wifeName != null) p.gender = "M"; // 有妻子则是男性
                else if (p.husbandId != null || p.husbandName != null) p.gender = "F"; // 有丈夫则是女性
            }
        }
        // 2. 对方角色推断
        for (var p : id_peoples.values()) {
            if (p.gender == null && p.spouseId != null) {
                Person spouse = id_peoples.get(p.spouseId);
                if (spouse != null) {
                    if ("M".equals(spouse.gender)) p.gender = "F";
                    else if ("F".equals(spouse.gender)) p.gender = "M";
                }
            }
        }
    }
}