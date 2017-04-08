package sk.tuke.mp.example;

import sk.tuke.mp.persistence.PersistenceManager;
import sk.tuke.mp.persistence.ReflectivePersistenceManager;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main2 {

  public static void main(String[] args) throws Exception {
    Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "root");
    PersistenceManager manager = new ReflectivePersistenceManager(connection);
    manager.initializeDatabase();
  }
}
