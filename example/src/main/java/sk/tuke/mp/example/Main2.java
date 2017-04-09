package sk.tuke.mp.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.tuke.mp.persistence.ReflectivePersistenceManager;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main2 {

  private static final Logger log = LoggerFactory.getLogger(Main2.class);

  public static void main(String[] args) throws Exception {
    Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "root");
    ReflectivePersistenceManager persistenceManager = new ReflectivePersistenceManager(connection);
//    persistenceManager.initializeDatabase();

    Person person = persistenceManager.myGet(Person.class, 1);
    Department department = person.getDepartment();

    log.info("person age: {}", person.getAge());
    log.info("department code: {}", department.getCode());
  }
}
