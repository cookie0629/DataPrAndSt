package org.example.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for person-ref complex type.
 * <p>person-ref (人物引用) 的 Java 类。
 * * <p>对应的 XML Schema 片段：
 * <pre>
 * &lt;complexType name="person-ref"&gt;
 * &lt;complexContent&gt;
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 * &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" /&gt;
 * &lt;/restriction&gt;
 * &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "person-ref")
public class PersonRef {

    // @XmlIDREF 指示 JAXB 将此属性作为对象引用处理，而不是简单的字符串。
    // 在解析 XML 时，JAXB 会查找具有匹配 @XmlID 的对象，并将其实例直接赋值给这个字段。
    @XmlAttribute(name = "id", required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object id; // 类型为 Object，因为它可以是一个 PersonType 实例(解析成功后)

    /**
     * 获取引用的对象 ID 或对象实例。
     */
    public Object getId() {
        return id;
    }

    /**
     * 设置引用的对象 ID 或对象实例。
     */
    public void setId(Object value) {
        this.id = value;
    }

}