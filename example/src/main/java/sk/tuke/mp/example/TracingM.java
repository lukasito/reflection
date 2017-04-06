package sk.tuke.mp.example;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by Joja on 07.03.2017.
 */
public class TracingM implements InvocationHandler{

    public static Object createProxy(Object obj, PrintWriter out) {
        return Proxy.newProxyInstance(obj.getClass().getClassLoader(),
                obj.getClass().getInterfaces(),
                new TracingM(obj, out));
    }

    private Object target;
    private PrintWriter out;

    private TracingM(Object obj, PrintWriter out) {
        target = obj;
        this.out = out;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        try {
            out.println(method.getName() + "(...) called");
            result = method.invoke(target, args);
        } catch (InvocationTargetException e) {
            out.println(method.getName() + " throws " + e.getCause());
            throw e.getCause();
        }
        out.println(method.getName() + " returns");
        return result;
    }
}
