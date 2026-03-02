package org.example.schema;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for children-type complex type.
 * <p>children-type 复杂类型的 Java 类定义。
 *
 * <p>对应的 XML Schema 片段如下：
 * <pre>
 * &lt;complexType name="children-type"&gt;
 * &lt;complexContent&gt;
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 * &lt;sequence&gt;
 * &lt;element name="daughter" type="{}person-ref" maxOccurs="unbounded" minOccurs="0"/&gt;
 * &lt;element name="son" type="{}person-ref" maxOccurs="unbounded" minOccurs="0"/&gt;
 * &lt;/sequence&gt;
 * &lt;/restriction&gt;
 * &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "children-type", propOrder = {
        "daughter",
        "son"
})
public class ChildrenType {

    // 存储女儿引用的列表
    protected List<PersonRef> daughter;
    // 存储儿子引用的列表
    protected List<PersonRef> son;

    /**
     * 获取 daughter (女儿) 属性的值。
     * * <p>
     * 这个访问器方法返回的是一个“实时列表”(live list)的引用，而不是快照。
     * 因此，对返回列表的任何修改都会直接反映在 Jakarta XML Binding 对象中。
     * 这就是为什么该属性没有 set 方法的原因。
     * * <p>
     * 例如，要添加一个新项，请这样做：
     * <pre>
     * getDaughter().add(newItem);
     * </pre>
     */
    public List<PersonRef> getDaughter() {
        if (daughter == null) {
            daughter = new ArrayList<PersonRef>();
        }
        return this.daughter;
    }

    /**
     * 获取 son (儿子) 属性的值。
     * * <p>
     * 同上，返回实时列表引用。
     * 要添加新项：
     * <pre>
     * getSon().add(newItem);
     * </pre>
     */
    public List<PersonRef> getSon() {
        if (son == null) {
            son = new ArrayList<PersonRef>();
        }
        return this.son;
    }

}