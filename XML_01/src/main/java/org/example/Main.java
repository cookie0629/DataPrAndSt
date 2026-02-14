package org.example;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        String outputFileName = "output.txt";
        try (InputStream stream = new FileInputStream("people.xml");
             // 创建一个写入文件的流
             PrintStream fileOut = new PrintStream(new FileOutputStream(outputFileName))) {

            System.out.println("parse XML...");
            HashMap<String, Person> data = new PeopleParser().parse(stream);

            // 1. 执行断言
            System.out.println("Assertions");
            Assertions assertions = new Assertions();
            assertions.assertChildren(data);
            assertions.assertSiblings(data);

            // 2. 将结果写入文件
            System.out.println("write rusult " + outputFileName + " ...");


            // 这里的逻辑是：只有返回 true 的人才会被写入文件
            java.util.function.Predicate<Person> isValid = p -> {
                // 1. 检查性别是否存在
                if (p.gender == null) {
                    return false; // 没性别，丢弃
                }

                // 2. 检查子女数量一致性 (如果有声明数量，必须匹配；没声明视为通过)
                if (p.childrenCount != null && p.childrenId.size() != p.childrenCount) {
                    return false; // 声明了数量但对不上，丢弃
                }

                // 3. 检查兄弟姐妹数量一致性
                if (p.siblingsCount != null && p.siblingsId.size() != p.siblingsCount) {
                    return false; // 声明了数量但对不上，丢弃
                }

                return true; // 所有检查都通过，保留
            };

            // 为了让文件看起来整洁，我们按 ID 排序后再写入
            List<Person> sortedPeople = data.values().stream()
                    .filter(isValid)
                    .sorted(Comparator.comparing(p -> p.id != null ? p.id : "NULL_ID"))
                    .collect(Collectors.toList());

            // 写入文件头
            fileOut.println("all people: " + data.size());
            for (Person p : sortedPeople) {
                fileOut.println(p.toString());
                fileOut.println("------------------------------------------");
            }

            System.out.println("all ready open " + outputFileName + " to checkout result");

        } catch (IOException | XMLStreamException e) {
            e.printStackTrace();
        }
    }
}