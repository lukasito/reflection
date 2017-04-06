package sk.tuke.mp.persistence.processors;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

public class FieldProcessor implements JpaProcessor {

  @Override
  public String apply(Element element) {
    StringBuilder stringBuilder = new StringBuilder();
    processIdAnnotation(element, stringBuilder);
    processColumnAnnotation(element, stringBuilder);
    processManyToOne(element, stringBuilder);
    return null;
  }

  private void processManyToOne(Element element, StringBuilder sb) {
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
      sb.append(name).append(" ").append(processTypeKind(element.asType()));
    }
  }

  private void processIdAnnotation(Element element, StringBuilder sb) {
    Id id = element.getAnnotation(Id.class);
    if (isDefined(id)) {
      sb.append("id ").append(processTypeKind(element.asType())).append(" NOT NULL");
    }
  }

  private void processColumnAnnotation(Element element, StringBuilder sb) {
    Column column = element.getAnnotation(Column.class);
    if (isDefined(column)) {
      String colName = column.name();
      if (colName.isEmpty()) {
        throw new ProcessingException("@Column.name missing!", element);
      }
      sb.append(colName).append(" ").append(processTypeKind(element.asType()));
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
