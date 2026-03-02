package org.example.schema;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for siblings-type complex type.
 * <p>siblings-type (兄弟姐妹类型) 的 Java 类。
 *
 * <p>对应的 XML Schema 片段：
 * <pre>
 * &lt;complexType name="siblings-type"&gt;
 * &lt;complexContent&gt;
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 * &lt;sequence&gt;
 * &lt;element name="sister" type="{}person-ref" maxOccurs="unbounded" minOccurs="0"/&gt;
 * &lt;element name="brother" type="{}person-ref" maxOccurs="unbounded" minOccurs="0"/&gt;
 * &lt;/sequence&gt;
 * &lt;/restriction&gt;
 * &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "siblings-type", propOrder = {
        "sister",
        "brother"
})
public class SiblingsType {

    protected List<PersonRef> sister;  // 姐妹引用列表
    protected List<PersonRef> brother; // 兄弟引用列表

    /**
     * 获取 sister (姐妹) 列表。
     * 返回实时列表，使用 getSister().add(item) 添加元素。
     */
    public List<PersonRef> getSister() {
        if (sister == null) {
            sister = new ArrayList<PersonRef>();
        }
        return this.sister;
    }

    /**
     * 获取 brother (兄弟) 列表。
     * 返回实时列表，使用 getBrother().add(item) 添加元素。
     */
    public List<PersonRef> getBrother() {
        if (brother == null) {
            brother = new ArrayList<PersonRef>();
        }
        return this.brother;
    }

}