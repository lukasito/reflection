package sk.tuke.mp.persistence.processors.mysql;

import org.apache.commons.lang3.StringUtils;
import sk.tuke.mp.persistence.processors.JpaProcessor;
import sk.tuke.mp.persistence.processors.ProcessingException;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.persistence.Entity;
import java.util.List;
import java.util.stream.Collectors;

class DDLEntityProcessor implements MysqlJpaProcessor {

  private final int INDENTATION = 2;

  private final JpaProcessor columnProcessor;
  private final JpaProcessor constraintProcessor;

  DDLEntityProcessor(JpaProcessor columnProcessor, JpaProcessor constraintProcessor) {
    this.columnProcessor = columnProcessor;
    this.constraintProcessor = constraintProcessor;
  }

  @Override
  public String apply(Element element) throws ProcessingException {
    String sql = "CREATE TABLE IF NOT EXISTS %s (\n%s\n)";
    String tableName = normalize(element.getAnnotation(Entity.class).name());

    if (tableName.isEmpty()) {
      throw new ProcessingException("Missing @Entity.name", element);
    }
    List<? extends Element> enclosedElements = element.getEnclosedElements();

    String columns = enclosedElements.stream()
      .filter(elem -> elem.getKind() == ElementKind.FIELD)
      .map(columnProcessor)
      .filter(notEmptyResult)
      .map(this::indent)
      .collect(Collectors.joining(",\n"));

    String constraints = enclosedElements.stream()
      .filter(elem -> elem.getKind() == ElementKind.FIELD)
      .map(constraintProcessor)
      .filter(notEmptyResult)
      .map(this::indent)
      .collect(Collectors.joining(",\n"));

    return String.format(sql, tableName, columns + ",\n" + constraints);
  }

  private String indent(String str) {
    return StringUtils.repeat(" ", INDENTATION) + str;
  }
}
