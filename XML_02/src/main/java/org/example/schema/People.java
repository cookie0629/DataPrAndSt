package org.example.schema;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * <p>匿名复杂类型的 Java 类。
 * * <p>对应的 XML Schema 片段：
 * <pre>
 * &lt;complexType&gt;
 * &lt;complexContent&gt;
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 * &lt;sequence minOccurs="0"&gt;
 * &lt;element name="person" type="{}person-type" maxOccurs="unbounded" minOccurs="0"/&gt;
 * &lt;/sequence&gt;
 * &lt;/restriction&gt;
 * &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "person"
})
// @XmlRootElement 注解表明这是 XML 文档的根节点，节点名为 "people"
@XmlRootElement(name = "people")
public class People {

    // 包含所有人的列表
    protected List<PersonType> person;

    /**
     * 获取 person 列表。
     * * <p>
     * 返回的是实时列表引用。对该列表的修改会影响 XML 结构。
     * * <p>
     * 允许的对象类型：
     * {@link PersonType }
     */
    public List<PersonType> getPerson() {
        if (person == null) {
            person = new ArrayList<PersonType>();
        }
        return this.person;
    }

}