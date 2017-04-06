package sk.tuke.mp.persistence;

import java.util.List;

public interface PersistenceManager {

    void initializeDatabase() throws PersistenceException;

    <T> List<T> getAll(Class<T> clazz) throws PersistenceException; //throws PersistenceException;

    <T> T get(Class<T> type, int id) throws PersistenceException; //throws PersistenceException;

    <T> List<T> getBy(Class<T> type, String fieldName, Object value) throws PersistenceException;

    int save(Object value) throws PersistenceException; //throws PersistenceException;
}
