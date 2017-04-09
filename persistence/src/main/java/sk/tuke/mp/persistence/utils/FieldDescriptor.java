package sk.tuke.mp.persistence.utils;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.lang.reflect.Field;

import static org.apache.commons.lang3.StringUtils.capitalize;

public class FieldDescriptor {

  private final Field underlying;
  private final String columnName;
  private final String enclosingEntityName;
  private final Category category;
  private final String joinColumn;
  private final String javaType;
  private final boolean isLazy;
  private final EntityDescriptor refEntity;
  private final String fieldName;
  private final String javaGetter;
  private final String javaSetter;

  public FieldDescriptor(Field column, String enclosingEntityName) {
    this.underlying = column;
    this.enclosingEntityName = enclosingEntityName;
    category = describeType(column);
    columnName = describeColName(column);
    fieldName = column.getName();
    javaType = describeJavaType(column);
    isLazy = describeLaziness(column);
    joinColumn = findJoinColumnName(column);
    refEntity = describeRefEntity(column);
    javaGetter = "get" + capitalize(fieldName);
    javaSetter = "set" + capitalize(fieldName);
  }

  private EntityDescriptor describeRefEntity(Field column) {
    if (category == Category.MANY_TO_ONE) {
      Class<?> targetEntity = column.getDeclaredAnnotation(ManyToOne.class).targetEntity();
      return new EntityDescriptor(targetEntity, this, isLazy);
    } else {
      return null;
    }
  }

  public boolean isId() {
    return category == Category.ID;
  }

  private String findJoinColumnName(Field column) {
    if (category == Category.MANY_TO_ONE) {
      return column.getDeclaredAnnotation(JoinColumn.class).name();
    }
    return null;
  }

  private boolean describeLaziness(Field column) {
    if (category == Category.MANY_TO_ONE) {
      ManyToOne manyToOne = column.getDeclaredAnnotation(ManyToOne.class);
      if (manyToOne.fetch() == FetchType.LAZY) {
        return true;
      }
    }
    return false;
  }

  private Category describeType(Field column) {
    if (column.getDeclaredAnnotation(Id.class) != null) {
      return Category.ID;
    } else if (column.getDeclaredAnnotation(Column.class) != null) {
      return Category.COLUMN;
    } else if (column.getDeclaredAnnotation(ManyToOne.class) != null) {
      return Category.MANY_TO_ONE;
    }
    throw new IllegalArgumentException("Unsupported jpa annotation on column: " + column.getName());
  }

  private String describeColName(Field column) {
    switch (category) {
      case ID:
        return "ID";
      case COLUMN:
        return column.getDeclaredAnnotation(Column.class).name();
    }
    return null;
  }

  private String describeJavaType(Field column) {
    return column.getType().getName();
  }

  public Field getUnderlying() {
    return underlying;
  }

  public String getColumnName() {
    return columnName;
  }

  public String getEnclosingEntityName() {
    return enclosingEntityName;
  }

  public Category getCategory() {
    return category;
  }

  public String getJavaType() {
    return javaType;
  }

  public String getJoinColumn() {
    return joinColumn;
  }

  public boolean isLazy() {
    return isLazy;
  }

  public EntityDescriptor asDescribedEntity() {
    return refEntity;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getJavaGetter() {
    return javaGetter;
  }

  public String getJavaSetter() {
    return javaSetter;
  }

  public enum Category {

    ID, MANY_TO_ONE, COLUMN
  }
}
