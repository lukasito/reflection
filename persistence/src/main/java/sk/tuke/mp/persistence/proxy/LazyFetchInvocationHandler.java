package sk.tuke.mp.persistence.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.tuke.mp.persistence.utils.EntityDescriptor;
import sk.tuke.mp.persistence.utils.FieldDescriptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

public class LazyFetchInvocationHandler implements InvocationHandler {

  private Object instance;
  private final Connection connection;
  private final int enclosingEntityId;
  private final EntityDescriptor lazyEntity;

  private static final Logger log = LoggerFactory.getLogger(LazyFetchInvocationHandler.class);

  public LazyFetchInvocationHandler(
    int enclosingEntityId, EntityDescriptor lazyEntity, Connection connection
  ) throws Exception {
    this.enclosingEntityId = enclosingEntityId;
    this.lazyEntity = lazyEntity;
    this.connection = connection;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (instance == null) {
      log.debug("Lazy instance hit, fetching..");
      instance = fetchLazy(lazyEntity);
    }
    log.debug("Invoking method: {}", method);
    return method.invoke(instance, args);
  }

  private Object fetchLazy(EntityDescriptor lazyEntity) throws Exception {
    String alias1 = "a1";
    String alias2 = "a2";
    List<String> sqlColumns = lazyEntity.getColumnNames();
    String sql = String.format("SELECT %s FROM %s %s JOIN %s %s WHERE %s.%s = ?;",
      sqlColumns.stream().map(col -> alias1 + "." + col).collect(Collectors.joining(", ")),
      lazyEntity.getEntityName(), alias1,
      lazyEntity.asDescribedField().getEnclosingEntityName(), alias2,
      alias2, "ID"
    );

    log.debug("SQL: {}", sql);
    PreparedStatement preparedStatement = connection.prepareStatement(sql);
    preparedStatement.setInt(1, enclosingEntityId);
    ResultSet resultSet = preparedStatement.executeQuery();
    resultSet.next(); // should be unique
    return instanceFromResultSet(lazyEntity, alias1, resultSet);
  }

  private Object instanceFromResultSet(
    EntityDescriptor lazyEntity, String alias1, ResultSet resultSet
  ) throws Exception {
    Class<?> refClass = lazyEntity.getUnderlying();
    Object instance = refClass.newInstance();

    for (FieldDescriptor fd : lazyEntity.getColumns()) {
      String colName = alias1 + "." + fd.getColumnName();
      Object value;
      switch (fd.getJavaType()) {
        case "int":
          value = resultSet.getInt(colName);
          refClass.getMethod(fd.getJavaSetter(), int.class).invoke(instance, value);
          break;
        case "double":
          value = resultSet.getDouble(colName);
          refClass.getMethod(fd.getJavaSetter(), double.class).invoke(instance, value);
          break;
        case "java.lang.String":
          value = resultSet.getString(colName);
          refClass.getMethod(fd.getJavaSetter(), String.class).invoke(instance, value);
          break;
      }
    }
    return instance;
  }
}
