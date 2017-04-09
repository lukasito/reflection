package sk.tuke.mp.persistence.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.tuke.mp.persistence.utils.EntityDescriptor;

import java.lang.reflect.Proxy;
import java.sql.Connection;

public class LazyFetchProxy {

  private static final Logger log = LoggerFactory.getLogger(LazyFetchProxy.class);

  public static Object create(
    int enclosingEntityId, EntityDescriptor entityDescriptor, Connection connection
  ) throws Exception {
    Class<?> underlying = entityDescriptor.getUnderlying();
    Class<?> firstInterface = findFirstInterface(underlying);
    if (entityDescriptor.isLazy()) {
      log.debug("Proxying entity {}", underlying);
      return firstInterface.cast(
        Proxy.newProxyInstance(underlying.getClassLoader(), new Class[]{firstInterface},
          new LazyFetchInvocationHandler(enclosingEntityId, entityDescriptor, connection))
      );
    } else {
      log.debug("Entity is not lazy, no proxy required...");
    }
    return firstInterface.cast(underlying.newInstance());
  }

  private static Class<?> findFirstInterface(Class<?> klass) {
    Class<?>[] interfaces = klass.getInterfaces();
    if (interfaces == null || interfaces.length == 0) {
      IllegalArgumentException iae = new IllegalArgumentException(String.format("Class <%s> has no interfaces", klass));
      log.error("Exception while finding interfaces", iae);
      throw iae;
    }
    return interfaces[0];
  }
}
