package sk.tuke.mp.persistence.proxy;

import sk.tuke.mp.persistence.utils.EntityDescriptor;

import java.lang.reflect.Proxy;
import java.sql.Connection;

public class LazyFetchProxy {

  private final Connection connection;

  public LazyFetchProxy(Connection connection) {
    this.connection = connection;
  }

  public <T> T enhanceClassWithProxy(Class<T> klass) throws Exception {
    EntityDescriptor entityDescriptor = new EntityDescriptor(klass);
    if (entityDescriptor.containsLazy()) {
      return klass.cast(Proxy.newProxyInstance(klass.getClassLoader(), new Class[]{klass},
        new LazyFetchInvocationHandler(entityDescriptor, connection)));
    }
    return klass.newInstance();
  }
}
