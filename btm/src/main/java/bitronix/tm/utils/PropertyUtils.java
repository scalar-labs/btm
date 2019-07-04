/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Smart reflection helper.
 *
 * @author Ludovic Orban
 */
public final class PropertyUtils {
  static final Logger log = LoggerFactory.getLogger(PropertyUtils.class);

    private PropertyUtils() {
    }

    /**
     * Set a direct or indirect property (dotted property: prop1.prop2.prop3) on the target object. This method tries
     * to be smart in the way that intermediate properties currently set to null are set if it is possible to create
     * and set an object. Conversions from propertyValue to the proper destination type are performed when possible.

     * There are nasty marginal case (see get/set ClonedProps in PropertyUtilsTest.java ), where parent getter do not
     * return an own object (but its clone) and a change on obtained the property object needs to be followed by call of
     * parent setter. This case leads to unsolvable cases in code.

     * First, read only properties are perfectly legal construction, and setters for them must not be called (raises an
     * exception) or not exists. ( Which is oposite scenario to ClonedProps )

     * There is the wanted feature that missing classes along property path are subject of creation if they are null and
     * set. Taking in account this feature and cases above we have a contradictory situation. Either we call setter to
     * assure to handle clonedProps case, but break read only properties. Or do not call parent setter after get and break
     * clonedProps. In other words we cannot safely say if the setter has to be called for clonedProps or not.

     * A configuation language ( OGNL, JavaScript, BeanShell ) is a solution of the problem. The frontier between
     * programming and configuration can be very thin.

     * @author Tomas Jura
     * @param target the target object on which to set the property.
     * @param propertyName the name of the property to set.
     * @param propertyValue the value of the property to set.
     * @throws PropertyException if an error happened while trying to set the property.
     */
    public static void setProperty(Object target, String propertyName, Object propertyValue) throws PropertyException {
      if (target == null) { throw new PropertyException("target is null"); }
      if (propertyName == null) { throw new PropertyException("propertyName is null"); }
      int dot = propertyName.indexOf('.');
      String prop = (dot==-1)? propertyName : propertyName.substring(0, dot);
      if (dot == -1) { // direct set property
        Method setter = null;
        String setterName = "set"+ prop.substring(0, 1).toUpperCase() + prop.substring(1);
        for (Method m : target.getClass().getMethods()) {
          if (m.getName().equals(setterName) && m.getReturnType().equals(void.class) && m.getParameterTypes().length == 1) {
            setter=m;
            break;
          }
        }
        if (setter == null) {
          if ( target instanceof Map ) {
            log.debug("cannot find property '{}' on {} and using it as a map with key {}",prop,target.getClass().getCanonicalName(),propertyName);
            @SuppressWarnings("unchecked") Map<String,Object> map=(Map<String,Object>) target;
            map.put(prop,propertyValue);
            return;
          }
          else throw new PropertyException("cannot set property '" + prop +"' for class " + target.getClass().getCanonicalName());
        }

        Class<?> parameterType = setter.getParameterTypes()[0];
        try {
          if (propertyValue != null) {
            Object transformedPropertyValue = transform(propertyValue, parameterType);
            setter.invoke(target, transformedPropertyValue);
          } else {
            setter.invoke(target);
          }
        } catch (IllegalAccessException ex) {
          throw new PropertyException("property '" + propertyName + "' is not accessible", ex);
        } catch (InvocationTargetException ex) {
          throw new PropertyException("property '" + propertyName + "' access threw an exception", ex);
        }
      }
      else { // sub property
        Method getter = null;
        String getterName = "get"+ prop.substring(0, 1).toUpperCase() + prop.substring(1);
        try {
          getter = target.getClass().getMethod(getterName);
          if ( getter.getReturnType().equals(void.class) ) { getter = null; }
        }
        catch (NoSuchMethodException ex) { }
        catch (SecurityException ex) { log.warn( "getter for {} found, but is not accesible. Ignoring it.",prop); }
        if (getter == null) {
          if ( target instanceof Map ) {
            log.warn("cannot find property '{}' on {} and using it as a map with key {}",prop,target.getClass().getCanonicalName(),propertyName);
            @SuppressWarnings("unchecked") Map<String,Object> map=(Map<String,Object>)target;
            map.put(propertyName,propertyValue);
            return;
          }
          else throw new PropertyException("cannot get property '" + prop +"' for class " + target.getClass().getCanonicalName());
        }

        Object newTarget;
        Boolean setterNeeded = false;
        try {
          newTarget = getter.invoke(target);
        } catch ( IllegalAccessException ex) {
          throw new PropertyException("cannot set property '" + propertyName + "' - '" + prop + "' is not accesible", ex);
        } catch ( InvocationTargetException ex) {
          throw new PropertyException("cannot set property '" + propertyName + "' - '" + prop + "'", ex);
        }
        Class<?> propertyType = getter.getReturnType();
        if (newTarget == null) { // then try to instantiate the object & set it in place
          setterNeeded = true;
          try {
            newTarget = propertyType.newInstance();
          } catch (InstantiationException ex) {
            throw new PropertyException("cannot set property '" + propertyName + "' - '" + prop + "' is null and cannot be auto-filled", ex);
          } catch (IllegalAccessException ex) {
            throw new PropertyException("cannot set property '" + propertyName + "' - '" + prop + "' is null and cannot be auto-filled", ex);
          }
        }
        setProperty( newTarget, propertyName.substring(dot+1), propertyValue );
        try {
          String setterName = "set"+ prop.substring(0, 1).toUpperCase() + prop.substring(1);
          Method setter = target.getClass().getMethod(setterName,propertyType);
          setter.invoke(target,newTarget);
        } catch (NoSuchMethodException ex )  {
          if (setterNeeded) {
            throw new PropertyException("cannot set property '" + propertyName + "' - '" + prop + "' is null and setter call failed.", ex);
          }
        } catch (SecurityException ex )  {
          if (setterNeeded) {
            throw new PropertyException("cannot set property '" + propertyName + "' - '" + prop + "' is null and setter call failed.", ex);
          }
        } catch (IllegalAccessException ex )  {
          if (setterNeeded) {
            throw new PropertyException("cannot set property '" + propertyName + "' - '" + prop + "' is null and setter call failed.", ex);
          }
        } catch (IllegalArgumentException ex )  {
          if (setterNeeded) {
            throw new PropertyException("cannot set property '" + propertyName + "' - '" + prop + "' is null and setter call failed.", ex);
          }
        } catch (InvocationTargetException ex )  {
          if (setterNeeded) {
            throw new PropertyException("cannot set property '" + propertyName + "' - '" + prop + "' is null and setter call failed.", ex);
          }
        }
      }
    }

    /**
     * Build a map of direct javabeans properties of the target object. Only read/write properties (ie: those who have
     * both a getter and a setter) are returned.
     * @param target the target object from which to get properties names.
     * @return a Map of String with properties names as key and their values
     * @throws PropertyException if an error happened while trying to get a property.
     */
    public static Map<String, Object> getProperties(Object target) throws PropertyException {
        Map<String, Object> properties = new HashMap<String, Object>();
        Class<?> clazz = target.getClass();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (method.getModifiers() == Modifier.PUBLIC && method.getParameterTypes().length == 0 && (name.startsWith("get") || name.startsWith("is")) && containsSetterForGetter(clazz, method)) {
                String propertyName;
                if (name.startsWith("get")) {
                    propertyName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                } else if (name.startsWith("is")) {
                    propertyName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                } else {
                    throw new PropertyException("method '" + name + "' is not a getter, thereof no setter can be found");
                }

                try {
                    Object propertyValue = method.invoke(target);
                    if (propertyValue != null && propertyValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> propertiesContent = getNestedProperties(propertyName, (Map<String, Object>) propertyValue);
                        properties.putAll(propertiesContent);
                    } else {
                        properties.put(propertyName, propertyValue);
                    }
                } catch (IllegalAccessException ex) {
                    throw new PropertyException("cannot set property '" + propertyName + "' - '" + name + "' is null and cannot be auto-filled", ex);
                } catch (InvocationTargetException ex) {
                    throw new PropertyException("cannot set property '" + propertyName + "' - '" + name + "' is null and cannot be auto-filled", ex);
                }

            } // if
        } // for
        return properties;
    }

    private static boolean containsSetterForGetter(Class<?> clazz, Method method) {
        String methodName = method.getName();
        String setterName;

        if (methodName.startsWith("get")) {
            setterName = "set" + methodName.substring(3);
        } else if (methodName.startsWith("is")) {
            setterName = "set" + methodName.substring(2);
        } else {
            throw new PropertyException("method '" + methodName + "' is not a getter, thereof no setter can be found");
        }

        Method[] methods = clazz.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(setterName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a direct or indirect property (dotted property: prop1.prop2.prop3) on the target object.
     * @param target the target object from which to get the property.
     * @param propertyName the name of the property to get.
     * @return the value of the specified property.
     * @throws PropertyException if an error happened while trying to get the property.
     */
    public static Object getProperty(Object target, String propertyName) throws PropertyException {
        String[] propertyNames = propertyName.split("\\.");
        Object currentTarget = target;
        for (int i = 0; i < propertyNames.length; i++) {
            String name = propertyNames[i];
            Object result = callGetter(currentTarget, name);
            if (result == null && i < propertyNames.length -1) {
                throw new PropertyException("cannot get property '" + propertyName + "' - '" + name + "' is null");
            }
            currentTarget = result;
        }

        return currentTarget;
    }

    /**
     * Set a {@link Map} of direct or indirect properties on the target object.
     * @param target the target object on which to set the properties.
     * @param properties a {@link Map} of String/Object pairs.
     * @throws PropertyException if an error happened while trying to set a property.
     */
    public static void setProperties(Object target, Map<String, Object> properties) throws PropertyException {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            setProperty(target, name, value);
        }
    }

    /**
     * Return a comma-separated String of r/w properties of the specified object.
     * @param obj the object to introspect.
     * @return a a comma-separated String of r/w properties.
     */
    public static String propertiesToString(Object obj) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> properties = new TreeMap<String, Object>(getProperties(obj));
        Iterator<String> it = properties.keySet().iterator();
        while (it.hasNext()) {
            String property = it.next();
            Object val = getProperty(obj, property);
            sb.append(property);
            sb.append("=");
            sb.append(val);
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Set a direct property on the target object. Conversions from propertyValue to the proper destination type
     * are performed whenever possible.
     * @param target the target object on which to set the property.
     * @param propertyName the name of the property to set.
     * @param propertyValue the value of the property to set.
     * @throws PropertyException if an error happened while trying to set the property.
     */
    private static void setDirectProperty(Object target, String propertyName, Object propertyValue) throws PropertyException {
        Method setter = getSetter(target, propertyName);
        Class<?> parameterType = setter.getParameterTypes()[0];
        try {
            if (propertyValue != null) {
                Object transformedPropertyValue = transform(propertyValue, parameterType);
                setter.invoke(target, transformedPropertyValue);
            } else {
                setter.invoke(target);
            }
        } catch (IllegalAccessException ex) {
            throw new PropertyException("property '" + propertyName + "' is not accessible", ex);
        } catch (InvocationTargetException ex) {
            throw new PropertyException("property '" + propertyName + "' access threw an exception", ex);
        }
    }

    private static Map<String, Object> getNestedProperties(String prefix, Map<String, Object> properties) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String name = entry.getKey();
            String value = (String) entry.getValue();
            result.put(prefix + '.' + name, value);
        }
        return result;
    }

    private static Object transform(Object value, Class<?> destinationClass) {
        if (value.getClass() == destinationClass) {
            return value;
        }

        if (    value.getClass() == boolean.class || value.getClass() == Boolean.class ||
                value.getClass() == byte.class || value.getClass() == Byte.class ||
                value.getClass() == short.class || value.getClass() == Short.class ||
                value.getClass() == int.class || value.getClass() == Integer.class ||
                value.getClass() == long.class || value.getClass() == Long.class ||
                value.getClass() == float.class || value.getClass() == Float.class ||
                value.getClass() == double.class || value.getClass() == Double.class
            ) {
            return value;
        }

        if ((destinationClass == boolean.class || destinationClass == Boolean.class)  &&  value.getClass() == String.class) {
            return Boolean.valueOf((String) value);
        }
        if ((destinationClass == byte.class || destinationClass == Byte.class)  &&  value.getClass() == String.class) {
            return new Byte((String) value);
        }
        if ((destinationClass == short.class || destinationClass == Short.class)  &&  value.getClass() == String.class) {
            return new Short((String) value);
        }
        if ((destinationClass == int.class || destinationClass == Integer.class)  &&  value.getClass() == String.class) {
            return new Integer((String) value);
        }
        if ((destinationClass == long.class || destinationClass == Long.class)  &&  value.getClass() == String.class) {
            return new Long((String) value);
        }
        if ((destinationClass == float.class || destinationClass == Float.class)  &&  value.getClass() == String.class) {
            return new Float((String) value);
        }
        if ((destinationClass == double.class || destinationClass == Double.class)  &&  value.getClass() == String.class) {
            return new Double((String) value);
        }

        throw new PropertyException("cannot convert values of type '" + value.getClass().getName() + "' into type '" + destinationClass + "'");
    }

    private static void callSetter(Object target, String propertyName, Object parameter) throws PropertyException {
        Method setter = getSetter(target, propertyName);
        try {
            setter.invoke(target, parameter);
        } catch (IllegalAccessException ex) {
            throw new PropertyException("property '" + propertyName + "' is not accessible", ex);
        } catch (InvocationTargetException ex) {
            throw new PropertyException("property '" + propertyName + "' access threw an exception", ex);
        }
    }

    private static Object callGetter(Object target, String propertyName) throws PropertyException {
        Method getter = getGetter(target, propertyName);
        try {
            return getter.invoke(target);
        } catch (IllegalAccessException ex) {
            throw new PropertyException("property '" + propertyName + "' is not accessible", ex);
        } catch (InvocationTargetException ex) {
            throw new PropertyException("property '" + propertyName + "' access threw an exception", ex);
        }
    }

    private static Method getSetter(Object target, String propertyName) {
        if (propertyName == null || "".equals(propertyName)) {
            throw new PropertyException("encountered invalid null or empty property name");
        }
        String setterName = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(setterName) && method.getReturnType().equals(void.class) && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        throw new PropertyException("no writeable property '" + propertyName + "' in class '" + target.getClass().getName() + "'");
    }

    private static Method getGetter(Object target, String propertyName) {
        String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String getterIsName = "is" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            if ((method.getName().equals(getterName) || method.getName().equals(getterIsName)) && !method.getReturnType().equals(void.class) && method.getParameterTypes().length == 0) {
                return method;
            }
        }
        throw new PropertyException("no readable property '" + propertyName + "' in class '" + target.getClass().getName() + "'");
    }

    private static Class<?> getPropertyType(Object target, String propertyName) {
        String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String getterIsName = "is" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            if ((method.getName().equals(getterName) || method.getName().equals(getterIsName)) && !method.getReturnType().equals(void.class) && method.getParameterTypes().length == 0) {
                return method.getReturnType();
            }
        }
        throw new PropertyException("no property '" + propertyName + "' in class '" + target.getClass().getName() + "'");
    }

}
