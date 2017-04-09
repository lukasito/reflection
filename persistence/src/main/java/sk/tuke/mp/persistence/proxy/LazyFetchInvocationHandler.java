package sk.tuke.mp.persistence.proxy;

import sk.tuke.mp.persistence.utils.EntityDescriptor;
import sk.tuke.mp.persistence.utils.FieldDescriptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LazyFetchInvocationHandler implements InvocationHandler {

  private Object instance;
  private final Map<String, Object> initialized = new HashMap<>();
  private final Map<String, FieldDescriptor> getterToFieldDescriptor;
  private final Connection connection;
  private final EntityDescriptor entityDescriptor;

  public LazyFetchInvocationHandler(EntityDescriptor entityDescriptor, Connection connection) throws Exception {
    this.entityDescriptor = entityDescriptor;
    this.connection = connection;
    List<FieldDescriptor> lazies = entityDescriptor.getColumns()
      .stream()
      .filter(FieldDescriptor::isLazy)
      .collect(Collectors.toList());

    getterToFieldDescriptor = lazies.stream().collect(Collectors.toMap(
      FieldDescriptor::getJavaGetter, fd -> fd
    ));
    instance = entityDescriptor.getUnderlying().newInstance();
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    if (initialized.containsKey(methodName)) {
      return initialized.get(methodName);
    } else {
      if (isLazyInvocation(methodName)) {
        Object obj = fetchLazy(instance, getterToFieldDescriptor.get(methodName));
        initialized.put(methodName, obj);
        return obj;
      }
    }
    return method.invoke(instance, method, args);
  }

  private boolean isLazyInvocation(String methodName) {
    return getterToFieldDescriptor.containsKey(methodName);
  }

  private Object fetchLazy(Object proxy, FieldDescriptor lazyEntity) throws Exception {
    String alias1 = "a1" ;
    String alias2 = "a2" ;
    EntityDescriptor refEntity = lazyEntity.getRefEntity();
    List<String> sqlColumns = refEntity.getColumnNames();
    String sql = String.format("SELECT %s FROM %s %s JOIN %s %s WHERE %s.%s = ?;",
      sqlColumns.stream().map(col -> alias1 + "." + col).collect(Collectors.joining(", ")),
      refEntity.getEntityName(), alias1,
      lazyEntity.getEnclosingEntityName(), alias2,
      alias2, "ID"
    );
    PreparedStatement preparedStatement = connection.prepareStatement(sql);
    preparedStatement.setInt(1, resolveEntityId(proxy));
    ResultSet resultSet = preparedStatement.executeQuery();
    resultSet.next();

    Class<?> refClass = refEntity.getUnderlying();
    Object refInstance = refClass.newInstance();

    for (FieldDescriptor fd : refEntity.getColumns()) {
      String colName = alias1 + "." + fd.getColumnName();
      Object value = null;
      switch (fd.getJavaType()) {
        case "int":
          value = resultSet.getInt(colName);
        case "double":
          value = resultSet.getDouble(colName);
        case "java.lang.String":
          value = resultSet.getString(colName);
      }
      if (value != null) {
        Method method = refClass.getMethod(fd.getJavaSetter());
        method.invoke(refInstance, value);
      }
    }
    return refInstance;
  }

  private int resolveEntityId(Object proxy) throws Exception {
    return (int) entityDescriptor.getUnderlying().getMethod("getId").invoke(proxy);
  }
}
