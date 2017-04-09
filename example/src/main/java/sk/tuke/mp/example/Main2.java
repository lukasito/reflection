package sk.tuke.mp.example;

import sk.tuke.mp.persistence.proxy.*;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main2 {

  public static void main(String[] args) throws Exception {
    Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "root");
    LazyFetchProxy lazyFetchProxy = new LazyFetchProxy(connection);

    Person enhancedPerson = lazyFetchProxy.enhanceClassWithProxy(Person.class);
    enhancedPerson.setAge(1);
    int age = enhancedPerson.getAge();
    System.out.println(age);

    System.out.println(enhancedPerson.getDepartment().getCode());
  }
}
