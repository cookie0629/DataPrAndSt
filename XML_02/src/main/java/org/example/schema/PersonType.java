package org.example.schema;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for person-type complex type.
 * <p>person-type (人物类型) 的 Java 类。
 * * <p>对应的 XML Schema 片段：
 * <pre>
 * &lt;complexType name="person-type"&gt;
 * &lt;complexContent&gt;
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 * &lt;sequence&gt;
 * &lt;element name="wife" type="{}person-ref" minOccurs="0"/&gt;
 * &lt;element name="husband" type="{}person-ref" minOccurs="0"/&gt;
 * &lt;element name="parents" type="{}parents-type" maxOccurs="unbounded" minOccurs="0"/&gt;
 * &lt;element name="siblings" type="{}siblings-type" maxOccurs="unbounded" minOccurs="0"/&gt;
 * &lt;element name="children" type="{}children-type" maxOccurs="unbounded" minOccurs="0"/&gt;
 * &lt;/sequence&gt;
 * &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" /&gt;
 * &lt;attribute name="name" type="{}name-type" /&gt;
 * &lt;attribute name="gender" type="{}gender-type" /&gt;
 * &lt;/restriction&gt;
 * &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "person-type", propOrder = {
        "wife",
        "husband",
        "parents",
        "siblings",
        "children"
})
public class PersonType {

    protected PersonRef wife;    // 妻子引用
    protected PersonRef husband; // 丈夫引用
    protected List<ParentsType> parents;   // 父母列表
    protected List<SiblingsType> siblings; // 兄弟姐妹列表
    protected List<ChildrenType> children; // 子女列表

    // @XmlID 标识这个属性是 XML 文档中的唯一标识符。
    // 其他对象可以通过 @XmlIDREF 引用这个字段的值。
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;

    @XmlAttribute(name = "name")
    protected String name;

    @XmlAttribute(name = "gender")
    protected GenderType gender;

    /**
     * 获取妻子引用。
     */
    public PersonRef getWife() {
        return wife;
    }

    /**
     * 设置妻子引用。
     */
    public void setWife(PersonRef value) {
        this.wife = value;
    }

    /**
     * 获取丈夫引用。
     */
    public PersonRef getHusband() {
        return husband;
    }

    /**
     * 设置丈夫引用。
     */
    public void setHusband(PersonRef value) {
        this.husband = value;
    }

    /**
     * 获取父母列表 (Live List)。
     */
    public List<ParentsType> getParents() {
        if (parents == null) {
            parents = new ArrayList<ParentsType>();
        }
        return this.parents;
    }

    /**
     * 获取兄弟姐妹列表 (Live List)。
     */
    public List<SiblingsType> getSiblings() {
        if (siblings == null) {
            siblings = new ArrayList<SiblingsType>();
        }
        return this.siblings;
    }

    /**
     * 获取子女列表 (Live List)。
     */
    public List<ChildrenType> getChildren() {
        if (children == null) {
            children = new ArrayList<ChildrenType>();
        }
        return this.children;
    }

    /**
     * 获取 ID (唯一标识)。
     */
    public String getId() {
        return id;
    }

    /**
     * 设置 ID。
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * 获取姓名。
     */
    public String getName() {
        return name;
    }

    /**
     * 设置姓名。
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * 获取性别 (枚举)。
     */
    public GenderType getGender() {
        return gender;
    }

    /**
     * 设置性别。
     */
    public void setGender(GenderType value) {
        this.gender = value;
    }

}