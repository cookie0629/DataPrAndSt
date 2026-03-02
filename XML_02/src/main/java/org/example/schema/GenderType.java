package org.example.schema;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for gender-type.
 * <p>gender-type 的 Java 类定义（枚举）。
 * * <p>对应的 XML Schema 片段：
 * <pre>
 * &lt;simpleType name="gender-type"&gt;
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 * &lt;enumeration value="M"/&gt;
 * &lt;enumeration value="F"/&gt;
 * &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 */
@XmlType(name = "gender-type")
@XmlEnum
public enum GenderType {

    M, // 男性
    F; // 女性

    public String value() {
        return name();
    }

    public static GenderType fromValue(String v) {
        return valueOf(v);
    }

}