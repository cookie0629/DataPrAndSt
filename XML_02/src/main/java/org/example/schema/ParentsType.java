package org.example.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for parents-type complex type.
 * <p>parents-type 复杂类型的 Java 类。
 * * <p>对应的 XML Schema 片段：
 * <pre>
 * &lt;complexType name="parents-type"&gt;
 * &lt;complexContent&gt;
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 * &lt;sequence&gt;
 * &lt;element name="mother" type="{}person-ref" minOccurs="0"/&gt;
 * &lt;element name="father" type="{}person-ref" minOccurs="0"/&gt;
 * &lt;/sequence&gt;
 * &lt;/restriction&gt;
 * &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "parents-type", propOrder = {
        "mother",
        "father"
})
public class ParentsType {

    protected PersonRef mother; // 母亲引用
    protected PersonRef father; // 父亲引用

    /**
     * 获取 mother 属性的值。
     */
    public PersonRef getMother() {
        return mother;
    }

    /**
     * 设置 mother 属性的值。
     */
    public void setMother(PersonRef value) {
        this.mother = value;
    }

    /**
     * 获取 father 属性的值。
     */
    public PersonRef getFather() {
        return father;
    }

    /**
     * 设置 father 属性的值。
     */
    public void setFather(PersonRef value) {
        this.father = value;
    }

}