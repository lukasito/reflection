package sk.tuke.mp.example;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "person")
public class Person {

  @Id
  private int id;
  @Column(name = "surname")
  private String surname;
  @Column(name = "name")
  private String name;
  @Column(name = "age")
  private int age;

  @ManyToOne(targetEntity = DepartmentImpl.class, fetch = FetchType.LAZY)
  @JoinColumn(name = "DEPT_ID")
  private Department department;

  public Person(String surname, String name, int age) {
    this.surname = surname;
    this.name = name;
    this.age = age;
  }

  public Person() {
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Department getDepartment() {
    return department;
  }

  public void setDepartment(Department department) {
    this.department = department;
  }

  @Override
  public String toString() {
    return String.format("Person %d: %s %s (%d)", id, surname, name, age);
  }
}
