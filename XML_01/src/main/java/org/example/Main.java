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

            // 写入文件头
            fileOut.println("all people: " + data.size());

            // 为了让文件看起来整洁，我们按 ID 排序后再写入
            List<Person> sortedPeople = data.values().stream()
                    .sorted(Comparator.comparing(p -> p.id != null ? p.id : "NULL_ID"))
                    .collect(Collectors.toList());

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