package sk.tuke.mp.persistence.processors.mysql;

import javax.lang.model.element.Element;
import javax.persistence.Entity;
import javax.persistence.Id;

public class DDLPrimaryKeyProcessor implements MysqlCompileTimeJpaProcessor {

  @Override
  public String apply(Element element) {
    Id id = element.getAnnotation(Id.class);
    if (id == null) {
      return EMPTY_RESULT;
    } else {
      String tableName = element.getEnclosingElement().getAnnotation(Entity.class).name();
      return String.format("ALTER TABLE %s ADD PRIMARY KEY (%s)", normalize(tableName), normalize("ID"));
    }
  }
}
