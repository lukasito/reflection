package sk.tuke.mp.example;

import sk.tuke.mp.persistence.PersistenceManager;
import sk.tuke.mp.persistence.ReflectivePersistenceManager;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class Main {
  public static void main(String[] args) throws Exception {

    Class.forName("com.mysql.jdbc.Driver").newInstance();
    Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "root");

    PersistenceManager manager = new ReflectivePersistenceManager(conn);

    //lazy fetching
    PrintWriter writer = new PrintWriter(System.out, true);
    PersistenceManager manager_tracing = (PersistenceManager) TracingM.createProxy(manager, writer);

    //vytvorenie databázových tabuliek pre ukladanie objektov
    manager_tracing.initializeDatabase();

    Department development = new DepartmentImpl("Development", "DVLP");
    Department marketing = new DepartmentImpl("Marketing", "MARK");

    Person hrasko = new Person("Janko", "Hrasko", 30);
    hrasko.setDepartment(development);
    Person mrkvicka = new Person("Jozko", "Mrkvicka", 25);
    mrkvicka.setDepartment(development);
    Person novak = new Person("Jan", "Novak", 45);
    novak.setDepartment(marketing);

    //uloženie objektu do databázy, alebo jeho aktualizácia v prípade, že sa v databáze už nachádza
    manager_tracing.save(hrasko);
    manager_tracing.save(mrkvicka);
    manager_tracing.save(novak);

    System.out.println("\n\n");

    //získanie konkrétneho objektu na základe jeho identifikátora
    List<Person> hraskoDB = manager_tracing.getBy(Person.class, "surname", novak.getSurname());
    for (Person p : hraskoDB) {
      System.out.println("Metoda <T> List<T> getBy(Class<T> type, String fieldName, Object value)\n" + p + "\n");
    }

    //získanie konkrétneho objektu na základe jeho identifikátora
    System.out.println("Metoda <T> T get(Class<T> type, int id)\n" + manager_tracing.get(Department.class, 2) + "\n");

    //získanie zoznamu všetkých objektov zadaného typu.
    List<Person> persons = manager_tracing.getAll(Person.class);
    for (Person person : persons) {
      System.out.println(person);
      System.out.println("  " + person.getDepartment());
    }

    conn.close();
  }
}
