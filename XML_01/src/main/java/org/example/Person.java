package org.example;

import java.util.HashSet;
import java.util.Set;

/**
 * 表示一个人的实体类。
 * 包含基本信息以及与他人的关系（父母、子女、兄弟姐妹、配偶）。
 * 使用 Set 集合是为了自动去重。
 */
public class Person {
    public String id;
    public String firstName;
    public String lastName;
    public String gender; // "M" or "F"

    // --- 父母关系 ---
    public Set<String> parentsId = new HashSet<>();
    public String motherId;
    public String fatherId;
    // 临时存储名字，用于后续通过名字查找 ID
    public Set<String> parentsName = new HashSet<>();
    public String motherName;
    public String fatherName;

    // --- 子女关系 ---
    public Set<String> childrenId = new HashSet<>();
    public Set<String> sonsId = new HashSet<>();
    public Set<String> daughtersId = new HashSet<>();
    public Set<String> childrenName = new HashSet<>();
    public Set<String> sonsName = new HashSet<>();
    public Set<String> daughtersName = new HashSet<>();

    // --- 兄弟姐妹关系 ---
    public Set<String> siblingsId = new HashSet<>();
    public Set<String> brothersId = new HashSet<>();
    public Set<String> sistersId = new HashSet<>();
    public Set<String> siblingsName = new HashSet<>();
    public Set<String> brothersName = new HashSet<>();
    public Set<String> sistersName = new HashSet<>();

    // --- 配偶关系 ---
    public String spouseId;
    public String husbandId;
    public String wifeId;
    public String spouseName;
    public String husbandName;
    public String wifeName;

    // 用于校验数据完整性的计数器
    public Integer childrenCount = null;
    public Integer siblingsCount = null;

    /**
     * 将传入的 Person 对象 p 的数据合并到当前对象中。
     * 这在 XML 中同一个人的信息分散在不同标签时非常有用。
     */
    void merge(Person p) {
        if (p == null) {
            return;
        }

        // 如果当前字段为空，则使用传入对象的字段
        if (id == null) id = p.id;
        if (firstName == null) firstName = p.firstName;
        if (lastName == null) lastName = p.lastName;
        if (gender == null) gender = p.gender;

        // 集合类型直接添加所有元素（Set 会自动去重）
        parentsId.addAll(p.parentsId);
        if (motherId == null) motherId = p.motherId;
        if (fatherId == null) fatherId = p.fatherId;
        parentsName.addAll(p.parentsName);
        if (motherName == null) motherName = p.motherName;
        if (fatherName == null) fatherName = p.fatherName;

        childrenId.addAll(p.childrenId);
        sonsId.addAll(p.sonsId);
        daughtersId.addAll(p.daughtersId);
        childrenName.addAll(p.childrenName);
        sonsName.addAll(p.sonsName);
        daughtersName.addAll(p.daughtersName);

        siblingsId.addAll(p.siblingsId);
        brothersId.addAll(p.brothersId);
        sistersId.addAll(p.sistersId);
        siblingsName.addAll(p.siblingsName);
        brothersName.addAll(p.brothersName);
        sistersName.addAll(p.sistersName);

        if (spouseId == null) spouseId = p.spouseId;
        if (husbandId == null) husbandId = p.husbandId;
        if (wifeId == null) wifeId = p.wifeId;
        if (spouseName == null) spouseName = p.spouseName;
        if (husbandName == null) husbandName = p.husbandName;
        if (wifeName == null) wifeName = p.wifeName;

        if (childrenCount == null) childrenCount = p.childrenCount;
        if (siblingsCount == null) siblingsCount = p.siblingsCount;
    }

    @Override
    public String toString() {
        // 稍微美化了输出格式
        return String.format("Person{ID='%s', Name='%s %s', Gender='%s', ChildrenNum=%d/%s, SiblingsNum=%d/%s, ChildrenIDs=%s}",
                id, firstName, lastName, gender,
                childrenId.size(), (childrenCount == null ? "?" : childrenCount),
                siblingsId.size(), (siblingsCount == null ? "?" : siblingsCount),
                childrenId);
    }
}