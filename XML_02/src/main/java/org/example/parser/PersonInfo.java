package org.example.parser;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 人物信息实体类
 * 用于存储从 XML 解析出来的个人属性及关系数据
 */
public class PersonInfo implements Serializable {
    // 基本身份信息
    public String id;           // 唯一标识符
    public String firstName;    // 名
    public String lastName;     // 姓
    public String gender;       // 性别 (M/F)

    // parents - 父母信息
    public Set<String> parentsId = new HashSet<String>(); // 父母 ID 集合
    public String motherId;     // 母亲 ID
    public String fatherId;     // 父亲 ID
    public Set<String> parentsName = new HashSet<String>(); // 父母姓名集合（用于无法获取ID时的备用）
    public String motherName;   // 母亲姓名
    public String fatherName;   // 父亲姓名

    // children - 子女信息
    public Set<String> childrenId = new HashSet<String>(); // 所有子女 ID
    public Set<String> sonsId = new HashSet<String>();     // 儿子 ID 集合
    public Set<String> daughtersId = new HashSet<String>(); // 女儿 ID 集合
    public Set<String> childrenName = new HashSet<String>(); // 子女姓名集合
    public Set<String> sonsName = new HashSet<String>();     // 儿子姓名集合
    public Set<String> daughtersName = new HashSet<String>(); // 女儿姓名集合

    // siblings - 兄弟姐妹信息
    public Set<String> siblingsId = new HashSet<String>(); // 所有兄弟姐妹 ID
    public Set<String> brothersId = new HashSet<String>(); // 兄弟 ID 集合
    public Set<String> sistersId = new HashSet<String>();  // 姐妹 ID 集合
    public Set<String> siblingsName = new HashSet<String>(); // 兄弟姐妹姓名集合
    public Set<String> brothersName = new HashSet<String>(); // 兄弟姓名集合
    public Set<String> sistersName = new HashSet<String>();  // 姐妹姓名集合

    // spouse - 配偶信息
    public String spouseId;    // 配偶 ID
    public String husbandId;   // 丈夫 ID
    public String wifeId;      // 妻子 ID
    public String spouseName;  // 配偶姓名
    public String husbandName; // 丈夫姓名
    public String wifeName;    // 妻子姓名

    // 校验用的计数字段
    public Integer childrenCount = null; // 记录的子女总数
    public Integer siblingsCount = null; // 记录的兄弟姐妹总数

    /**
     * 将另一个 PersonInfo 对象的数据合并到当前对象中
     * 主要用于当发现两个 PersonInfo 实例实际上指向同一个人时
     * @param p 另一个 PersonInfo 对象
     */
    void merge(PersonInfo p) {
        if (p == null) {
            return;
        }

        // 合并基本信息 (如果当前为空则采纳对方的值)
        if (id == null) id = p.id;
        if (firstName == null) firstName = p.firstName;
        if (lastName == null) lastName = p.lastName;
        if (gender == null) gender = p.gender;

        // 合并父母信息
        parentsId.addAll(p.parentsId);
        if (motherId == null) motherId = p.motherId;
        if (fatherId == null) fatherId = p.fatherId;
        parentsName.addAll(p.parentsName);
        if (motherName == null) motherName = p.motherName;
        if (fatherName == null) fatherName = p.fatherName;

        // 合并子女信息
        childrenId.addAll(p.childrenId);
        sonsId.addAll(p.sonsId);
        daughtersId.addAll(p.daughtersId);
        childrenName.addAll(p.childrenName);
        sonsName.addAll(p.sonsName);
        daughtersName.addAll(p.daughtersName);

        // 合并兄弟姐妹信息
        siblingsId.addAll(p.siblingsId);
        brothersId.addAll(p.brothersId);
        sistersId.addAll(p.sistersId);
        siblingsName.addAll(p.siblingsName);
        brothersName.addAll(p.brothersName);
        sistersName.addAll(p.sistersName);

        // 合并配偶信息
        if (spouseId == null) spouseId = p.spouseId;
        if (husbandId == null) husbandId = p.husbandId;
        if (wifeId == null) wifeId = p.wifeId;
        if (spouseName == null) spouseName = p.spouseName;
        if (husbandName == null) husbandName = p.husbandName;
        if (wifeName == null) wifeName = p.wifeName;

        // 合并统计数量
        if (childrenCount == null) childrenCount = p.childrenCount;
        if (siblingsCount == null) siblingsCount = p.siblingsCount;
    }

    /**
     * 获取全名
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "ID: " + id + " " + firstName + " " + lastName + " " + gender +
                " | siblings: " + siblingsId.toString() + " " + sistersId.toString() + " " + brothersId +
                " | spouse: " + fatherId + " " + motherId + " " + parentsId +
                " | sn: " + siblingsCount + " cn: " + childrenCount;
    }
}