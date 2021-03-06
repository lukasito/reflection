package sk.tuke.mp.persistence.utils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityDescriptor {

  private final Class<?> underlying;
  private final String entityName;
  private final List<FieldDescriptor> columns;
  private final boolean isLazy;
  private final FieldDescriptor descriptor;

  public EntityDescriptor(Class<?> underlying) {
    this(underlying, null, false);
  }

  public EntityDescriptor(Class<?> underlying, FieldDescriptor descriptor, boolean isLazy) {
    this.underlying = underlying;
    this.isLazy = isLazy;
    this.descriptor = descriptor;
    entityName = describeEntity(underlying);
    columns = describeEntityColumns(underlying);
  }

  public List<String> getColumnNames() {
    return columns.stream()
      .map(FieldDescriptor::getColumnName)
      .collect(Collectors.toList());
  }

  public FieldDescriptor getId() {
    return columns.stream()
      .filter(FieldDescriptor::isId)
      .findFirst()
      .orElse(null);
  }

  public void setId(Object instance, Object id) throws Exception {
    FieldDescriptor idDescriptor = getId();
    underlying.getMethod(idDescriptor.getJavaSetter(), int.class).invoke(instance, id);
  }

  public FieldDescriptor asDescribedField() {
    return descriptor;
  }

  public Class<?> getFirstInterface() {
    Class<?>[] interfaces = underlying.getInterfaces();
    return interfaces == null || interfaces.length == 0
      ? null
      : interfaces[0];
  }

  public List<FieldDescriptor> getLazies() {
    return columns.stream()
      .filter(FieldDescriptor::isLazy)
      .collect(Collectors.toList());
  }

  public boolean containsLazy() {
    return columns.stream().anyMatch(FieldDescriptor::isLazy);
  }

  public boolean isLazy() {
    return isLazy;
  }

  private List<FieldDescriptor> describeEntityColumns(Class<?> entity) {
    return Stream.of(entity.getDeclaredFields())
      .filter(idAnnot.or(colAnnot).or(manyToOneAnnot))
      .map(field -> new FieldDescriptor(field, entityName))
      .collect(Collectors.toList());
  }

  private static final Predicate<Field> colAnnot = field ->
    field.getDeclaredAnnotation(Column.class) != null;

  private static final Predicate<Field> idAnnot = field ->
    field.getDeclaredAnnotation(Id.class) != null;

  private static final Predicate<Field> manyToOneAnnot = field ->
    field.getDeclaredAnnotation(ManyToOne.class) != null;

  private String describeEntity(Class<?> klass) {
    Entity entity = klass.getDeclaredAnnotation(Entity.class);
    if (entity == null) {
      throw new IllegalArgumentException("Class has no @Entity annotation: " + klass);
    } else {
      return entity.name();
    }
  }

  public Class<?> getUnderlying() {
    return underlying;
  }

  public String getEntityName() {
    return entityName;
  }

  public List<FieldDescriptor> getColumns() {
    return columns;
  }
}
