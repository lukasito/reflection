package sk.tuke.mp.persistence.processors.mysql;

import sk.tuke.mp.persistence.processors.ProcessingException;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

class DDLColumnProcessor implements MysqlJpaProcessor {

  @Override
  public String apply(Element element) {
    return processColumn(element);
  }

  private String processColumn(Element element) {
    return processIdAnnotation(element);
  }

  private String processIdAnnotation(Element element) {
    Id id = element.getAnnotation(Id.class);
    if (isDefined(id)) {
      return createColumnDefinition("ID", element.asType(), false);
    } else {
      return processColumnAnnotation(element);
    }
  }

  private String processColumnAnnotation(Element element) {
    Column column = element.getAnnotation(Column.class);
    if (isDefined(column)) {
      String colName = column.name();
      if (colName.isEmpty()) {
        throw new ProcessingException("@Column.name missing!", element);
      }
      return createColumnDefinition(colName, element.asType());
    } else {
      return processManyToOne(element);
    }
  }

  private String processManyToOne(Element element) {
    // Uni-directional support only
    ManyToOne manyToOne = element.getAnnotation(ManyToOne.class);
    if (isDefined(manyToOne)) {
      JoinColumn joinColumn = element.getAnnotation(JoinColumn.class);
      if (joinColumn == null) {
        throw new ProcessingException("@JoinColumn annotation missing on @ManyToOne entity", element);
      }

      String name = joinColumn.name();
      if (name.isEmpty()) {
        throw new ProcessingException("@JoinColumn.name needs to be specified!", element);
      }
      return createColumnDefinition(name, element.asType());
    } else {
      return EMPTY_RESULT;
    }
  }

  private String createColumnDefinition(String column, TypeMirror type, boolean nullable) {
    String columnDefinition = createColumnDefinition(column, type);
    return nullable ? columnDefinition : columnDefinition + " NOT NULL";
  }

  private String createColumnDefinition(String name, TypeMirror element) {
    return String.format("%s %s", normalize(name), processTypeKind(element));
  }

  private String processTypeKind(TypeMirror typeMirror) {
    TypeKind typeKind = typeMirror.getKind();
    String sqlType;
    if (typeKind.isPrimitive()) {
      sqlType = processPrimitive(typeKind);
    } else {
      sqlType = "VARCHAR(255)";
    }
    return sqlType;
  }

  private String processPrimitive(TypeKind typeKind) {
    switch (typeKind) {
      case DOUBLE:
        return "DOUBLE";
      case INT:
        return "INT";
    }
    return null;
  }

  private boolean isDefined(Object obj) {
    return obj != null;
  }
}
