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
package bitronix.tm.resource.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base class for Proxy InvocationHandlers.  Maintains a method cache
 * for swift delegation to either the overridden methods (implemented
 * in a sub-class of this class) or the underlying delegate class'
 * methods.  Makes proxying an interface almost completely painless.
 * <p/>
 *
 * @author brettw
 */
public abstract class BaseProxyHandlerClass implements InvocationHandler {
    private static final ConcurrentMap<Class, ConcurrentMap<Method, Method>> classMethodCache = new ConcurrentHashMap<Class, ConcurrentMap<Method, Method>>();
    private final ConcurrentMap<Method, Method> methodCache;

    public BaseProxyHandlerClass() {
        ConcurrentMap<Method, Method> methodCache = classMethodCache.get(this.getClass());
        if (methodCache == null) {
            methodCache = new ConcurrentHashMap<Method, Method>();
            ConcurrentMap<Method, Method> previous = classMethodCache.putIfAbsent(this.getClass(), methodCache);
            if (previous != null)
                methodCache = previous;
        }
        this.methodCache = methodCache;
    }

    /**
     * Implementation of the InvocationHandler interface.
     *
     * @see java.lang.reflect.InvocationHandler
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // If the method is directly overridden by "this" (i.e. sub-class)
            // class call "this" class' Method with "this" object, otherwise
            // call the non-overridden Method with the proxied object
            Method delegatedMethod = getDelegatedMethod(method);
            return delegatedMethod.invoke(isOurMethod(delegatedMethod) ? this : getProxiedDelegate(), args);
        } catch (InvocationTargetException ite) {
            // the InvocationTargetException's target actually is the exception thrown by the delegate
            // throw this one to avoid the caller to receive proxy-related exceptions
            throw ite.getTargetException();
        }
    }

    /**
     * Get the overridden Method for the super-class of this base class, if it
     * exists. Otherwise, the method provided is not overridden by us and should
     * go to the underlying proxied class.
     * <p/>
     * This method will return the original Method that was passed in, or if
     * the method is overridden by us it will return the Method from "this"
     * class.  Where "this" is actually the sub-class of this class.
     *
     * @param method the Method object to map
     * @return the Method object that should be invoked, either ours
     *         (overridden) or the underlying proxied object
     */
    private Method getDelegatedMethod(Method method) {
        Method delegated = methodCache.get(method);
        if (delegated != null) {
            return delegated;
        }

        try {
            Class[] parameterTypes = method.getParameterTypes();
            delegated = this.getClass().getMethod(method.getName(), parameterTypes);
        } catch (Exception ex) {
            delegated = method;
        }

        Method previous = methodCache.putIfAbsent(method, delegated);
        if (previous != null) {
            delegated = previous;
        }

        return delegated;
    }

    /**
     * Check whether the specified Method is overridden by us or not.
     *
     * @param method the Method object to check
     * @return true if the Method is ours, false if it belongs to the proxied
     *         Class
     */
    private boolean isOurMethod(Method method) {
        return this.getClass().equals(method.getDeclaringClass());
    }

    /**
     * Must be implemented by the sub-class of this class.  This method
     * should return the "true" object to be delegated to in the case
     * that the method is not overridden by the sub-class.
     *
     * @return the true delegate object
     * @throws Exception can throw any exception if desired
     */
	public abstract Object getProxiedDelegate() throws Exception;
}
