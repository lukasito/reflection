package sk.tuke.mp.persistence.processors;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Optional;

class DDLColumnProcessor implements JpaProcessor {

  @Override
  public String apply(Element element) {
    return processColumn(element);
  }

  private String processColumn(Element element) {
    return processIdAnnotation(element)
      .orElse("");
  }

  private Optional<String> processIdAnnotation(Element element) {
    Id id = element.getAnnotation(Id.class);
    if (isDefined(id)) {
      return Optional.of("id " + processTypeKind(element.asType()) + " NOT NULL");
    } else {
      return processColumnAnnotation(element);
    }
  }

  private Optional<String> processColumnAnnotation(Element element) {
    Column column = element.getAnnotation(Column.class);
    if (isDefined(column)) {
      String colName = column.name();
      if (colName.isEmpty()) {
        throw new ProcessingException("@Column.name missing!", element);
      }
      return Optional.of(colName + " " + processTypeKind(element.asType()));
    } else {
      return processManyToOne(element);
    }
  }

  private Optional<String> processManyToOne(Element element) {
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
      return Optional.of(name + " " + processTypeKind(element.asType()));
    } else {
      return Optional.empty();
    }
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
