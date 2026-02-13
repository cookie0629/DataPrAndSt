package org.example;

import java.util.Map;

/**
 * 数据校验类。
 * 用于检查解析后的数据是否符合预期（例如：实际找到的子女数量是否等于声明的数量）。
 */
public class Assertions {

    /**
     * 校验兄弟姐妹数量。
     * 如果 XML 中声明了 siblingsCount，但实际解析出的 siblingsId 数量不符，打印警告。
     */
    public void assertSiblings(Map<String, Person> id_peoples) {
        for (var p : id_peoples.values()) {
            if (p.siblingsCount != null && p.siblingsId.size() != p.siblingsCount) {
                System.out.println("SIBLINGS ASSERT FAIL (兄弟姐妹数量不匹配): " + p);
            }
        }
    }

    /**
     * 校验子女数量。
     */
    public void assertChildren(Map<String, Person> id_peoples) {
        for (var p : id_peoples.values()) {
            if (p.childrenCount != null && p.childrenId.size() != p.childrenCount) {
                System.out.println("CHILDREN ASSERT FAIL (子女数量不匹配): " + p);
            }
        }
    }
}