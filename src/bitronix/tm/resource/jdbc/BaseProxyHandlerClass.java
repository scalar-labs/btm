package bitronix.tm.resource.jdbc;

import java.lang.reflect.*;
import java.util.*;

public abstract class BaseProxyHandlerClass implements InvocationHandler {
	private Map methodCache = new HashMap();

	/**
	 * Implementation of the InvocationHandler interface.
	 * 
	 * @see java.lang.reflect.InvocationHandler
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			// If the method is directly overridden by "this" (i.e. super) class
			// call it with "this" object, otherwise call it with the proxied object
			Method delegatedMethod = (Method) getDelegatedMethod(method);
			return delegatedMethod.invoke(isOurMethod(delegatedMethod) ? this : getProxiedDelegate(), args);
		} catch (InvocationTargetException ite) {
			throw ite.getTargetException();
		}
	}

	/**
	 * Get the overridden Method for the super-class of this
	 * base class, if it exists.  Otherwise, the method
	 * provided is not overridden by us and should go to the
	 * underlying proxied class.
	 *
	 * @param method the Method object to map
	 * @return the Method object that should be invoked, either
	 *    ours (overridden) or the underlying proxied object
	 */
	private Method getDelegatedMethod(Method method) {
		Class thisClass = this.getClass();
		synchronized (thisClass) {
			Method delegated = (Method) methodCache.get(method);
			if (delegated != null) {
				return delegated;
			}

			try {
				Class[] parameterTypes = method.getParameterTypes();
				delegated = thisClass.getMethod(method.getName(), parameterTypes);
			} catch (Exception e) {
				delegated = method;
			}
			methodCache.put(method, delegated);
			return delegated;
		}
	}

	/**
	 * Check whether the specified Method is overridden by us
	 * or not.
	 *
	 * @param method the Method object to check
	 * @return true if the Method is ours, false if it belongs
	 *    to the proxied Class
	 */
	private boolean isOurMethod(Method method) {
		return this.getClass().equals(method.getDeclaringClass());
	}

	public abstract Object getProxiedDelegate() throws Exception;
}
