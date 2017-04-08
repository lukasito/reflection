package sk.tuke.mp.persistence;

/**
 * Created by Joja on 05.03.2017.
 */
public class PersistenceException extends Exception {

  public PersistenceException(Throwable cause) {
    super(cause);
  }

  public PersistenceException(String s) {
    super(s);
  }
}
