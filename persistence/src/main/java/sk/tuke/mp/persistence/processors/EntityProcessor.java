package sk.tuke.mp.persistence.processors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.persistence.Entity;
import java.util.List;
import java.util.stream.Collectors;

public class EntityProcessor implements JpaProcessor {

  private final JpaProcessor columnProcessor;
  private final JpaProcessor constraintProcessor;

  EntityProcessor(JpaProcessor columnProcessor, JpaProcessor constraintProcessor) {
    this.columnProcessor = columnProcessor;
    this.constraintProcessor = constraintProcessor;
  }

  @Override
  public String apply(Element element) throws ProcessingException {
    String sql = "CREATE TABLE IF NOT EXISTS %s (\n%s\n)";
    String tableName = element.getAnnotation(Entity.class).name();

    if (tableName.isEmpty()) {
      throw new ProcessingException("Missing @Entity.name", element);
    }
    List<? extends Element> enclosedElements = element.getEnclosedElements();

    String columns = enclosedElements.stream()
      .filter(elem -> elem.getKind() == ElementKind.FIELD)
      .map(columnProcessor)
      .collect(Collectors.joining(",\n"));

    String constraints = enclosedElements.stream()
      .filter(elem -> elem.getKind() == ElementKind.FIELD)
      .map(constraintProcessor)
      .collect(Collectors.joining(",\n"));
    return String.format(sql, tableName, columns + ",\n" + constraints);
  }
}
