package org.example;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.example.parser.PeopleParser;
import org.example.parser.PersonInfo;
import org.example.schema.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 主程序入口类
 * 流程：读取 resources -> 解析(Parse) -> 转换模型 -> 建立关联 -> 输出 XML (Marshal)
 */
public class Main {
    // 存储解析器解析出的原始数据 Map (Key: ID, Value: PersonInfo)
    private static final Map<String, PersonInfo> parsedData = new HashMap<>();

    // 存储转换后的 JAXB 对象 Map (Key: ID, Value: PersonType)
    // 这里存储的是最终用于生成 XML 的对象引用，用于处理 @XmlIDREF 关系
    private static final Map<String, PersonType> collectedData = new HashMap<>();

    public static void main(String[] args) {
        // 1: 解析阶段 - 直接解析 people.xml
        try {
            // 1. 从 resources 文件夹加载 people.xml 文件流
            InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("people.xml");

            if (inputStream == null) {
                System.err.println("can't find people.xml！");
                return;
            }

            // 2. 初始化解析器并解析数据
            PeopleParser parser = new PeopleParser();
            // parse 方法包含了数据清洗和逻辑推断 (normalize)
            Map<String, PersonInfo> data = parser.parse(inputStream);

            // 3. 将解析结果存入静态 Map
            parsedData.putAll(data);
            System.out.println("all " + parsedData.size() + " datas");

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // --- 转换阶段 ---

        // 创建根节点对象 <people>
        People people = new People();

        // 第一轮循环：创建所有 PersonType 基础对象
        // 注意：这里必须先创建好所有的对象实例，才能在下一步中建立它们之间的引用关系
        for (var info : parsedData.values()) {
            PersonType person = new PersonType();
            setPersonInfo(person, info); // 设置 ID、姓名、性别
            collectedData.put(info.id, person);
        }

        // 第二轮循环：建立人物之间的关联 (配偶、子女、父母、兄弟姐妹)
        // 此时 collectedData 中已经有了所有的 PersonType 对象，可以安全地进行引用
        for (var person : collectedData.values()) {
            setSpouse(person);    // 设置配偶引用
            setChildren(person);  // 设置子女引用
            setParents(person);   // 设置父母引用
            setSiblings(person);  // 设置兄弟姐妹引用
        }

        // 将处理好的所有 PersonType 对象添加到根节点列表中
        people.getPerson().addAll(collectedData.values());

        // --- 输出阶段 (JAXB Marshalling) ---
        try {
            // 初始化 JAXB 上下文
            JAXBContext jc = JAXBContext.newInstance(People.class);
            Marshaller writer = jc.createMarshaller();

            // 设置 Schema 工厂，用于校验输出是否符合 XSD 定义
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // 检查 schema 文件是否存在
            File schemaFile = new File("src/main/resources/schema.xsd");
            if (schemaFile.exists()) {
                // 设置 schema，这样输出的 XML 会包含 schemaLocation 并且符合规范
                writer.setSchema(schemaFactory.newSchema(schemaFile));
            } else {
                System.out.println("can't find schema.xsd");
            }

            // 设置属性：格式化输出 (自动缩进换行)
            writer.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // 定义输出文件路径并执行序列化 (Marshal)
            File outputFile = new File("src/main/resources/output.xml");
            writer.marshal(people, outputFile); // 将 Java 对象转换为 XML 文件
            System.out.println("XML succeed!");

        } catch (JAXBException | SAXException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置基础信息：ID, Name, Gender
     */
    private static void setPersonInfo(PersonType person, PersonInfo info) {
        person.setId(info.id);
        // 使用 full name 组合名和姓
        person.setName(info.getFullName());

        // 设置性别，需要将字符串 "M"/"F" 转换为 GenderType 枚举
        if (info.gender != null) {
            person.setGender(GenderType.fromValue(info.gender));
        }
    }

    /**
     * 设置配偶关系
     * 利用 @XmlIDREF 特性，我们只需要将目标 PersonType 对象传入 PersonRef
     */
    private static void setSpouse(PersonType person) {
        var info = parsedData.get(person.getId());

        // 如果是男性且有妻子 ID
        if (info.wifeId != null) {
            PersonRef personRef = new PersonRef();
            // 注意：这里传入的是 collectedData.get(id)，即传入的是 PersonType 对象本身
            // JAXB 会自动将其处理为指向该对象 ID 的引用字符串
            personRef.setId(collectedData.get(info.wifeId));
            person.setWife(personRef);

        } else if (info.husbandId != null) {
            PersonRef personRef = new PersonRef();
            personRef.setId(collectedData.get(info.husbandId));
            person.setHusband(personRef);
        }
    }

    /**
     * 设置子女关系 (Daughter / Son)
     */
    private static void setChildren(PersonType person) {
        var info = parsedData.get(person.getId());
        ChildrenType childrenType = new ChildrenType();

        // 添加女儿引用
        for (var daughterId : info.daughtersId) {
            PersonRef personRef = new PersonRef();
            personRef.setId(collectedData.get(daughterId));
            childrenType.getDaughter().add(personRef);
        }
        // 添加儿子引用
        for (var sonId : info.sonsId) {
            PersonRef personRef = new PersonRef();
            personRef.setId(collectedData.get(sonId));
            childrenType.getSon().add(personRef);
        }

        // 只有当确实有子女数据时，才将 children 节点添加到 person 中
        // 避免生成空的 <children /> 标签
        if (!childrenType.getDaughter().isEmpty() || !childrenType.getSon().isEmpty()) {
            person.getChildren().add(childrenType);
        }
    }

    /**
     * 设置父母关系 (Mother / Father)
     */
    private static void setParents(PersonType person) {
        PersonInfo info = parsedData.get(person.getId());
        ParentsType parentsType = new ParentsType();

        if (info.motherId != null) {
            PersonRef personRef = new PersonRef();
            personRef.setId(collectedData.get(info.motherId));
            parentsType.setMother(personRef);
        }
        if (info.fatherId != null) {
            PersonRef personRef = new PersonRef();
            personRef.setId(collectedData.get(info.fatherId));
            parentsType.setFather(personRef);
        }

        // 只有当有父母信息时才添加节点
        if (parentsType.getMother() != null || parentsType.getFather() != null) {
            person.getParents().add(parentsType);
        }
    }

    /**
     * 设置兄弟姐妹关系 (Sister / Brother)
     */
    private static void setSiblings(PersonType person) {
        var info = parsedData.get(person.getId());
        SiblingsType siblingsType = new SiblingsType();

        for (var sisterId : info.sistersId) {
            PersonRef personRef = new PersonRef();
            personRef.setId(collectedData.get(sisterId));
            siblingsType.getSister().add(personRef);
        }
        for (var brotherId : info.brothersId) {
            PersonRef personRef = new PersonRef();
            personRef.setId(collectedData.get(brotherId));
            siblingsType.getBrother().add(personRef);
        }

        if (!siblingsType.getSister().isEmpty() || !siblingsType.getBrother().isEmpty()) {
            person.getSiblings().add(siblingsType);
        }
    }
}