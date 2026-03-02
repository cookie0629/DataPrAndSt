package org.example.schema;

import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * 这个对象包含用于创建 schema 包中生成的 Java 内容接口和 Java 元素接口的工厂方法。
 * <p>ObjectFactory 允许你以编程方式构建 XML 内容的 Java 表示形式。
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * 创建一个新的 ObjectFactory，用于创建 schema 包的派生类实例。
     */
    public ObjectFactory() {
    }

    /**
     * 创建 {@link People } 的实例 (根元素)
     */
    public People createPeople() {
        return new People();
    }

    /**
     * 创建 {@link PersonType } 的实例 (人物主体)
     */
    public PersonType createPersonType() {
        return new PersonType();
    }

    /**
     * 创建 {@link PersonRef } 的实例 (人物引用)
     */
    public PersonRef createPersonRef() {
        return new PersonRef();
    }

    /**
     * 创建 {@link ParentsType } 的实例 (父母集合)
     */
    public ParentsType createParentsType() {
        return new ParentsType();
    }

    /**
     * 创建 {@link SiblingsType } 的实例 (兄弟姐妹集合)
     */
    public SiblingsType createSiblingsType() {
        return new SiblingsType();
    }

    /**
     * 创建 {@link ChildrenType } 的实例 (子女集合)
     */
    public ChildrenType createChildrenType() {
        return new ChildrenType();
    }

}