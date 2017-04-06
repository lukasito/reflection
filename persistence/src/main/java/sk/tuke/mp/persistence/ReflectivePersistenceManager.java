package sk.tuke.mp.persistence;

import java.lang.reflect.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.util.Objects;

/**
 * Created by Joja on 28.02.2017.
 */
public class ReflectivePersistenceManager implements PersistenceManager {

    private Connection conn = null;
    private Class[] classes;
    private int info = 0;

    public ReflectivePersistenceManager(Connection connection, Class... classes) {
        this.conn = connection;
        this.classes = classes;
    }

    @Override
    public void initializeDatabase() throws PersistenceException {

        try {
            if (info > 0)
                System.out.println("Connecting to database...");

            //open connection
            Statement stm = conn.createStatement();
            DatabaseMetaData dbm = conn.getMetaData();

            if (info > 0)
                System.out.println("Connected database successfully...");

            for (Class concreteClass : classes) {
                ResultSet keys = dbm.getExportedKeys(null, "APP", concreteClass.getSimpleName().toUpperCase());
                while (keys.next()) {
                    stm.execute("ALTER TABLE " + keys.getString(7) + " DROP FOREIGN KEY " + keys.getString(8));
                }
            }

            for (Class concreteClass : classes) {
                ResultSet tables = dbm.getTables(null, "APP", concreteClass.getSimpleName().toUpperCase(), null);
                if (tables.next()) {
                    if (info > 0) System.out.println("    DROP TABLE " + concreteClass.getSimpleName());
                    stm.execute("    DROP TABLE " + concreteClass.getSimpleName());
                }
            }

            for (Class concreteClass : classes) {

                String sql = "CREATE TABLE " + concreteClass.getSimpleName() + "(";
                String constraints = "";

                Field[] fields = concreteClass.getDeclaredFields();
                for (Field field : fields) {
                    switch (field.getGenericType().toString()) {

                        case "class java.lang.String":
                            sql = sql + field.getName() + " varchar(255),";
                            if (info > 1) System.out.println("   -" + field.getName() + " String");
                            break;
                        case "double":
                            sql = sql + field.getName() + " double,";
                            if (info > 1) System.out.println("   -" + field.getName() + " Double");
                            break;
                        case "int":
                            sql = sql + field.getName() + " int,";
                            if (info > 1) System.out.println("   -" + field.getName() + " Int");
                            break;

                        default:
                            boolean isFieldFound = false;

                            for (Class oneClass : classes) {
                                if (Objects.equals(oneClass.toString(), field.getGenericType().toString())) {
                                    isFieldFound = true;
                                    sql = sql + field.getName() + " int,";
                                    constraints +=
                                            " CONSTRAINT " + field.getName() + " FOREIGN KEY("
                                                    + field.getName() + ") REFERENCES " + oneClass.getSimpleName() + "(id),";
                                    if (info > 1)
                                        System.out.println("   -" + field.getName() + " " + oneClass.getSimpleName());
                                }

                                if (field.getType().isInterface()) {
                                    for (Class oneInterface : oneClass.getInterfaces()) {
                                        if (Objects.equals(oneInterface.getSimpleName(), field.getType().getSimpleName())) {
                                            isFieldFound = true;
                                            sql = sql + field.getName() + " int,";
                                            constraints += " CONSTRAINT " + field.getName() + " FOREIGN KEY(" + field.getName() + ") REFERENCES " + oneClass.getSimpleName() + "(id),";
                                            if (info > 1)
                                                System.out.println("   -" + field.getName() + " " + oneClass.getSimpleName());
                                        }
                                    }
                                }
                            }

                            if (!isFieldFound) {
                                throw new PersistenceException("Field " + field.getName() + " of type " + field.getType().getName() + " in Class " + concreteClass.getName() + " is not supported by ReflectivePersistenceManager.");
                            }
                            break;
                    }
                }
                sql = sql + constraints + " PRIMARY KEY (id))";
                if (info > 0) System.out.println(sql);

                stm.execute(sql);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

//            String sql1 = "CREATE TABLE PERSON " +
//                    "(id INTEGER not NULL," +
//                    "name VARCHAR(50)," +
//                    "surname VARCHAR(100)," +
//                    "age INTEGER," +
//                    "departmentID INTEGER," +
//                    "PRIMARY KEY (id))";
//
//            String sql2 = "CREATE TABLE DEPARTMENT" +
//                    "(id INTEGER not NULL," +
//                    "name VARCHAR(200)," +
//                    "code VARCHAR(50)," +
//                    "PRIMARY KEY (id))";
//
//            stmt.executeUpdate(sql1);
//            System.out.println("Created table PERSON in given database.");
//
//            stmt.executeUpdate(sql2);
//            System.out.println("Created table DEPARTMENT in given database.");


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
            return getGroup(all,clazz);
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
        } catch (InstantiationException | InvocationTargetException | SQLException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> T standardGet(Class<T> type, int id) throws IllegalAccessException, InstantiationException, SQLException, NoSuchMethodException, InvocationTargetException, PersistenceException {
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
            ResultSet all = statement.executeQuery("SELECT * FROM " + type.getSimpleName() + " WHERE " + fieldName + "='" + value+"'");

            return getGroup(all,type);
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
