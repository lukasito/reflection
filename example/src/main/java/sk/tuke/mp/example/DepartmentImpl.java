package sk.tuke.mp.example;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "department")
public class DepartmentImpl implements Department {

  @Id
  private int id;
  @Column(name = "name")
  private String name;
  @Column(name = "code")
  private String code;

  public DepartmentImpl(String name, String code) {
    this.name = name;
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }


  public String toString() {
    return String.format("Department %d: %s (%s)", id, name, code);
  }
}
