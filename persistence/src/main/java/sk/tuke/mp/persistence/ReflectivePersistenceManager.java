package sk.tuke.mp.persistence;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Joja on 28.02.2017.
 */
public class ReflectivePersistenceManager implements PersistenceManager {

  private final Connection conn;
  private Class[] classes;
  private int info = 0;

  public ReflectivePersistenceManager(Connection connection) {
    this.conn = connection;
  }

  @Override
  public void initializeDatabase() throws PersistenceException {
    try {
      String sql = loadDDL();
      Statement stm = conn.createStatement();

      for (String command : sql.split(";")) {
        if (isNotEmptyQuery(command)) {
          stm.addBatch(command);
        }
      }
      stm.executeBatch();
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

  private boolean isNotEmptyQuery(String query) {
    return query.replaceAll("\\s|;", "").isEmpty();
  }

  private String loadDDL() throws IOException {
    InputStream stream = ReflectivePersistenceManager.class.getClassLoader().getResourceAsStream("ddl.sql");
    StringWriter output = new StringWriter();
    IOUtils.copy(stream, output, StandardCharsets.UTF_8);
    return output.toString();
  }

  @Override
  public <T> List<T> getAll(Class<T> clazz) throws PersistenceException {
    try {
      if (clazz.isInterface()) {
        for (Class oneClass : classes) {
          for (Class oneInterface : oneClass.getInterfaces()) {
            if (Objects.equals(oneInterface.getSimpleName(), clazz.getSimpleName())) {
              clazz = oneClass;
            }
          }
        }
      }
      Statement statement = conn.createStatement();
      ResultSet all = statement.executeQuery("SELECT * FROM " + clazz.getSimpleName());
      if (info > 0) System.out.println("SELECT * FROM " + clazz.getSimpleName());
      return getGroup(all, clazz);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    throw new PersistenceException("getAll Failed");
  }

  @Override
  public <T> T get(Class<T> type, int id) throws PersistenceException {
    try {
      if (type.isInterface()) {
        for (Class oneClass : classes) {
          for (Class oneInterface : oneClass.getInterfaces()) {
            if (Objects.equals(oneInterface.getSimpleName(), type.getSimpleName())) {
              Object object = Proxy.newProxyInstance(oneClass.getClassLoader(), oneClass.getInterfaces(),
                new InvocationHandler() {
                  T object = null;

                  @Override
                  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (Objects.equals(method.getName(), "toString2")) {
                      return method.invoke(oneClass.newInstance(), args);
                    } else {
                      if (object == null) {
                        if (info > 0)
                          System.out.println("Lazy loading " + method.getName());
                        object = standardGet((Class<T>) oneClass, id);
                      } else {
                        if (info > 1)
                          System.out.println("Using proxy " + method.getName());
                      }

                      return method.invoke(object, args);
                    }
                  }
                });
              return (T) object;
            }
          }
        }
        return null;
      } else {
        return standardGet(type, id);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private <T> T standardGet(Class<T> type, int id) throws Exception {
    Statement statement = conn.createStatement();
    T object = type.newInstance();
    Field[] fields = object.getClass().getDeclaredFields();
    ResultSet all = statement.executeQuery("SELECT * FROM " + type.getSimpleName() + " WHERE ID=" + id);
    if (info > 0) System.out.println("SELECT * FROM " + type.getSimpleName() + " WHERE ID=" + id);
    if (all.next()) {
      if (info > 1) System.out.println("loading " + type.getSimpleName());
      for (Field field : fields) {
        switch (field.getType().toString()) {
          case "int":
            if (info > 1)
              System.out.println("    -" + field.getName() + "=" + all.getString(field.getName()));
            object.getClass().getMethod(getSetterName(field), field.getType()).invoke(object, all.getInt(field.getName()));
            break;
          case "double":
            if (info > 1)
              System.out.println("    -" + field.getName() + "=" + all.getString(field.getName()));
            object.getClass().getMethod(getSetterName(field), field.getType()).invoke(object, all.getDouble(field.getName()));
            break;
          case "class java.lang.String":
            if (info > 1)
              System.out.println("    -" + field.getName() + "=" + all.getString(field.getName()));
            object.getClass().getMethod(getSetterName(field), field.getType()).invoke(object, all.getString(field.getName()));
            break;
          default:
            if (info > 1)
              System.out.println("    -" + field.getName() + "=" + all.getString(field.getName()));
            object.getClass().getMethod(getSetterName(field), field.getType()).invoke(object, get(field.getType(), all.getInt(field.getName())));
            all = statement.executeQuery("SELECT * FROM " + type.getSimpleName() + " WHERE ID=" + id);
            all.next();
            break;
        }
      }
      if (info > 1) System.out.println("loaded " + type.getSimpleName());
    }
    return object;
  }


  @Override
  public <T> List<T> getBy(Class<T> type, String fieldName, Object value) throws PersistenceException {
    try {
      if (type.isInterface()) {
        for (Class oneClass : classes) {
          for (Class oneInterface : oneClass.getInterfaces()) {
            if (Objects.equals(oneInterface.getSimpleName(), type.getSimpleName())) {
              type = oneClass;
            }
          }
        }
      }
      Statement statement = conn.createStatement();
      if (info > 0) System.out.println("SELECT * FROM " + type.getSimpleName() + " WHERE " + fieldName + "=" + value);
      ResultSet all = statement.executeQuery("SELECT * FROM " + type.getSimpleName() + " WHERE " + fieldName + "='" + value + "'");

      return getGroup(all, type);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    throw new PersistenceException("getBy Failed");
  }

  private <T> List<T> getGroup(ResultSet all, Class<T> type) throws PersistenceException {
    List<T> objects = new ArrayList<>();
    List<String> sqls = new ArrayList<>();
    List<Class> types = new ArrayList<>();
    try {
      while (all.next()) {
        T object = type.newInstance();
        Field[] fields = object.getClass().getDeclaredFields();
        if (info > 1) System.out.println("loading " + type.getSimpleName());
        for (Field field : fields) {
          switch (field.getType().toString()) {
            case "int":
              if (info > 1)
                System.out.println("    -" + field.getName() + "=" + all.getString(field.getName()));
              object.getClass().getMethod(getSetterName(field), field.getType())
                .invoke(object, all.getInt(field.getName()));
              break;
            case "double":
              if (info > 1)
                System.out.println("    -" + field.getName() + "=" + all.getString(field.getName()));
              object.getClass().getMethod(getSetterName(field), field.getType())
                .invoke(object, all.getDouble(field.getName()));
              break;
            case "class java.lang.String":
              if (info > 1)
                System.out.println("    -" + field.getName() + "=" + all.getString(field.getName()));
              object.getClass().getMethod(getSetterName(field), field.getType())
                .invoke(object, all.getString(field.getName()));
              break;
            default:
              if (info > 1)
                System.out.println("    -" + field.getName() + "=" + all.getInt(field.getName()));
              for (Class oneClass : classes) {
                if (Objects.equals(oneClass.toString(), field.getGenericType().toString())) {
                  String sql = objects.size() + "|" + getSetterName(field) + "=" + all.getInt(field.getName());
                  sqls.add(sql);
                  types.add(field.getType());
                }
                if (field.getType().isInterface()) {
                  for (Class oneInterface : oneClass.getInterfaces()) {
                    if (Objects.equals(oneInterface.getSimpleName(), field.getType().getSimpleName())) {
                      String sql = objects.size() + "|" + getSetterName(field) + "=" + all.getInt(field.getName());
                      sqls.add(sql);
                      types.add(field.getType());
                    }
                  }
                }
              }
              break;
          }
        }
        objects.add(object);
        if (info > 1) System.out.println("loaded " + type.getSimpleName());
      }
      for (String sql : sqls) {
        int objectIndex = Integer.valueOf(sql.substring(0, sql.lastIndexOf("|")));
        String name = sql.substring(sql.lastIndexOf("|") + 1, sql.lastIndexOf("="));
        int fieldId = Integer.valueOf(sql.substring(sql.lastIndexOf("=") + 1, sql.length()));
        objects.get(objectIndex).getClass().getMethod(name, types.get(sqls.indexOf(sql)))
          .invoke(objects.get(objectIndex), get(types.get(sqls.indexOf(sql)), fieldId));
      }

    } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | SQLException | IllegalAccessException e) {
      e.printStackTrace();
    }

    return objects;
  }


  @Override
  public int save(Object value) throws PersistenceException {
    int result = 0;
    try {
      Statement statement = conn.createStatement();
      if (0 == getId(value)) {
        ResultSet maxId = statement.executeQuery("SELECT MAX(id) FROM " + value.getClass().getSimpleName());
        if (info > 0) System.out.println("SELECT MAX(id) FROM " + value.getClass().getSimpleName());
        if (maxId.next()) {
          value.getClass().getMethod("setId", int.class)
            .invoke(value, maxId.getInt(1) + 1);
        }
        String sql = "INSERT INTO " + value.getClass().getSimpleName() + " VALUES (";
        Field[] fields = value.getClass().getDeclaredFields();
        for (Field field : fields) {
          switch (field.getGenericType().toString()) {
            case "int":
              sql += value.getClass().getMethod(getGetterName(field)).invoke(value) + ",";
              break;

            case "double":
              sql += value.getClass().getMethod(getGetterName(field)).invoke(value) + ",";
              break;

            case "class java.lang.String":
              sql += "'" + value.getClass().getMethod(getGetterName(field)).invoke(value) + "',";
              break;

            default:
              for (Class oneClass : classes) {
                if (Objects.equals(oneClass.toString(), field.getGenericType().toString())) {
                  sql += save(value.getClass().getMethod(getGetterName(field)).invoke(value)) + ",";
                }
              }
              if (field.getType().isInterface()) {
                for (Class oneClass : classes) {
                  for (Class oneInterface : oneClass.getInterfaces()) {
                    if (Objects.equals(oneInterface.getSimpleName(), field.getType().getSimpleName())) {
                      sql += save(value.getClass().getMethod(getGetterName(field)).invoke(value)) + ",";

                    }
                  }
                }
              }
              break;
          }
        }
        sql = sql.substring(0, sql.length() - 1) + ")";
        if (info > 0) System.out.println(sql);
        statement.execute(sql);
      } else {
        if (info > 1) System.out.println("saving object with id not 0");
        isChanged(value);
      }
      result = (int) value.getClass().getMethod("getId").invoke(value);
    } catch (SQLException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      e.printStackTrace();
    }
    return result;
  }

  private int getId(Object value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return (int) value.getClass().getMethod("getId").invoke(value);
  }

  private int getInt(Object value, Field field) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return (int) value.getClass().getMethod(getGetterName(field)).invoke(value);
  }

  private Double getDouble(Object value, Field field) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return (Double) value.getClass().getMethod(getGetterName(field)).invoke(value);
  }

  private String getString(Object value, Field field) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return (String) value.getClass().getMethod(getGetterName(field)).invoke(value);
  }

  private String getGetterName(Field field) {
    return "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
  }

  private String getSetterName(Field field) {
    return "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
  }

  private boolean isChanged(Object value) throws SQLException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Statement statement = conn.createStatement();

    if (Proxy.isProxyClass(value.getClass())) {
      value = Proxy.getInvocationHandler(value).getClass().getMethod("getOriginal").invoke(Proxy.getInvocationHandler(value));
    }
    if (info > 0) System.out.println(
      "SELECT * FROM " + value.getClass().getSimpleName() + " WHERE id=" + getId(value));
    ResultSet maxId = statement.executeQuery(
      "SELECT * FROM " + value.getClass().getSimpleName() + " WHERE id=" + getId(value));
    List<String> sqls = new ArrayList<>();
    if (maxId.next()) {
      for (Field field : value.getClass().getDeclaredFields()) {
        switch (field.getGenericType().toString()) {
          case "int":
            if (maxId.getInt(field.getName()) != getInt(value, field)) {
              sqls.add("UPDATE " + value.getClass().getSimpleName() + " SET " + field.getName() + "=" + getInt(value, field) + " WHERE ID=" + getId(value));
            }
            break;

          case "double":
            if (maxId.getDouble(field.getName()) != getDouble(value, field)) {
              sqls.add("UPDATE " + value.getClass().getSimpleName() + " SET " + field.getName() + "=" + getDouble(value, field) + " WHERE ID=" + getId(value));
            }
            break;

          case "class java.lang.String":
            if (!Objects.equals(maxId.getString(field.getName()), getString(value, field))) {
              sqls.add("UPDATE " + value.getClass().getSimpleName() + " SET " + field.getName() + "='" + getString(value, field) + "' WHERE ID=" + getId(value));
            }
            break;

          default:
            if (info > 1) System.out.println("CHANGE: "
              + field.getName() + "=" + getId(value.getClass()
              .getMethod("get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1))
              .invoke(value)) + " inDB=" + maxId.getInt(field.getName()));
            if (getId(value.getClass().getMethod("get"
              + field.getName().substring(0, 1).toUpperCase()
              + field.getName().substring(1)).invoke(value)) == maxId.getInt(field.getName())) {
              isChanged(value.getClass().getMethod("get"
                + field.getName().substring(0, 1).toUpperCase()
                + field.getName().substring(1)).invoke(value));
            } else {
              sqls.add("UPDATE "
                + value.getClass().getSimpleName() + " SET " + field.getName()
                + "=" + getInt(value, field) + " WHERE ID=" + getId(value));
            }
            break;
        }
      }
    }
    for (String sql : sqls) {
      if (info > 0) System.out.println(sql);
      statement.execute(sql);
    }

    return false;
  }
}
