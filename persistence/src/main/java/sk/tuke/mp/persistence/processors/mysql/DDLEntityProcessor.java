package sk.tuke.mp.persistence.processors.mysql;

import org.apache.commons.lang3.StringUtils;
import sk.tuke.mp.persistence.processors.CompileTimeJpaProcessor;
import sk.tuke.mp.persistence.processors.CompileTimeProcessingException;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.persistence.Entity;
import java.util.List;
import java.util.stream.Collectors;

class DDLEntityProcessor implements MysqlCompileTimeJpaProcessor {

  private final int INDENTATION = 2;

  private final CompileTimeJpaProcessor columnProcessor;

  DDLEntityProcessor(CompileTimeJpaProcessor columnProcessor) {
    this.columnProcessor = columnProcessor;
  }

  @Override
  public String apply(Element element) throws CompileTimeProcessingException {
    String sql = "CREATE TABLE IF NOT EXISTS %s (\n%s\n)";
    String tableName = normalize(element.getAnnotation(Entity.class).name());

    if (tableName.isEmpty()) {
      throw new CompileTimeProcessingException("Missing @Entity.name", element);
    }
    List<? extends Element> enclosedElements = element.getEnclosedElements();

    String columns = enclosedElements.stream()
      .filter(elem -> elem.getKind() == ElementKind.FIELD)
      .map(columnProcessor)
      .filter(notEmptyResult)
      .map(this::indent)
      .collect(Collectors.joining(",\n"));

    return String.format(sql, tableName, columns);
  }

  private String indent(String str) {
    return StringUtils.repeat(" ", INDENTATION) + str;
  }
}
