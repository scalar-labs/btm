package bitronix.tm.resource.jdbc;

import java.lang.reflect.*;
import java.util.*;

/**
 * Base class for Proxy InvocationHandlers.  Maintains a method cache
 * for swift delegation to either the overridden methods (implemented
 * in a sub-class of this class) or the underlying delegate class'
 * methods.  Makes proxying an interface almost completely painless.
 *
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author brettw
 */
public abstract class BaseProxyHandlerClass implements InvocationHandler {
	private static Map classMethodCache = new HashMap();
	private Map methodCache;

	public BaseProxyHandlerClass() {
		synchronized (this.getClass()) {
			methodCache = (Map) classMethodCache.get(this.getClass());
			if (methodCache == null) {
				methodCache = new HashMap();
				classMethodCache.put(this.getClass(), methodCache);
			}
		}
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
			Method delegatedMethod = (Method) getDelegatedMethod(method);
			return delegatedMethod.invoke(isOurMethod(delegatedMethod) ? this : getProxiedDelegate(), args);
		} catch (InvocationTargetException ite) {
			throw ite.getTargetException();
		}
	}

	/**
	 * Get the overridden Method for the super-class of this base class, if it
	 * exists. Otherwise, the method provided is not overridden by us and should
	 * go to the underlying proxied class.
	 *
	 * This method will return the original Method that was passed in, or if
	 * the method is overridden by us it will return the Method from "this"
	 * class.  Where "this" is actually the sub-class of this class.
	 * 
	 * @param method the Method object to map
	 * @return the Method object that should be invoked, either ours
	 *         (overridden) or the underlying proxied object
	 */
	private synchronized Method getDelegatedMethod(Method method) {
		Method delegated = (Method) methodCache.get(method);
		if (delegated != null) {
			return delegated;
		}

		try {
			Class[] parameterTypes = method.getParameterTypes();
			delegated = this.getClass().getMethod(method.getName(), parameterTypes);
		} catch (Exception e) {
			delegated = method;
		}
		methodCache.put(method, delegated);
		return delegated;
	}

	/**
	 * Check whether the specified Method is overridden by us or not.
	 * 
	 * @param method
	 *            the Method object to check
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
