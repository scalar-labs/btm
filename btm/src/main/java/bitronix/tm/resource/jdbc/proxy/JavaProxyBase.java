/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */

package bitronix.tm.resource.jdbc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brett Wooldridge
 */
public abstract class JavaProxyBase<T> implements InvocationHandler {

    private static Map<Method, String> methodKeyMap = new ConcurrentHashMap<Method, String>();

    private T proxy;

    protected T delegate;

    protected abstract Map<String, Method> getMethodMap();

    protected T getProxy() {
        return proxy;
    }

    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        this.proxy = (T) proxy;

        try {
            Method ourMethod = getMethodMap().get(getMethodKey(method));
            if (ourMethod != null) {
                return ourMethod.invoke(this, args);
            }
    
            return method.invoke(delegate, args);
        }
        catch (InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }

    protected static Map<String, Method> createMethodMap(Class<?> clazz) {
        HashMap<String, Method> selfMethodMap = new HashMap<String, Method>();
        for (Method method : clazz.getDeclaredMethods()) {
            if ((method.getModifiers() & Method.PUBLIC) == Method.PUBLIC) {
                selfMethodMap.put(getMethodKey(method), method);
            }
        }
        return selfMethodMap;
    }

    private static String getMethodKey(Method method) {
        String key = methodKeyMap.get(method);
        if (key != null) {
            return key;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(method.getReturnType().getName())
          .append(method.getName());
        for (Class<?> type : method.getParameterTypes()) {
            sb.append(type.getName());
        }
        key = sb.toString();
        methodKeyMap.put(method, key);
        return key;
    }
}
