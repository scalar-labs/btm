package bitronix.tm.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Smart reflection helper.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class PropertyUtils {

    /**
     * Set a direct or indirect property (dotted property: prop1.prop2.prop3) on the target object. This method tries
     * to be smart in the way that intermediate properties currently set to null are set if it is possible to create
     * and set an object. Conversions from propertyValue to the proper destination type are performed when possible.
     * @param target the target object on which to set the property.
     * @param propertyName the name of the property to set.
     * @param propertyValue the value of the property to set.
     * @throws IllegalAccessException
     * @throws PropertyException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static void setProperty(Object target, String propertyName, Object propertyValue) throws IllegalAccessException, InvocationTargetException, PropertyException {
        String[] propertyNames = propertyName.split("\\.");
        Object currentTarget = target;
        for (int i = 0; i < propertyNames.length -1; i++) {
            String name = propertyNames[i];
            Object result = callGetter(currentTarget, name);
            if (result == null) {
                Class propertyType = getPropertyType(target, name);
                try {
                    result = propertyType.newInstance();
                } catch (InstantiationException ex) {
                    throw new PropertyException("cannot set property '" + propertyName + "' - '" + name + "' is null and cannot be auto-filled", ex);
                }
                callSetter(currentTarget, name, result);
            }
            currentTarget = result;
        }

        String lastPropertyName = propertyNames[propertyNames.length - 1];
        if (currentTarget instanceof Properties) {
            Properties p = (Properties) currentTarget;
            p.setProperty(lastPropertyName, propertyValue.toString());
        }
        else {
            setDirectProperty(currentTarget, lastPropertyName, propertyValue);
        }
    }

    /**
     * Get a direct or indirect property (dotted property: prop1.prop2.prop3) on the target object.
     * @param target the target object from which to get the property.
     * @param propertyName the name of the property to get.
     * @return the value of the specified property.
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static Object getProperty(Object target, String propertyName) throws IllegalAccessException, InvocationTargetException {
        String[] propertyNames = propertyName.split("\\.");
        Object currentTarget = target;
        for (int i = 0; i < propertyNames.length; i++) {
            String name = propertyNames[i];
            Object result = callGetter(currentTarget, name);
            if (result == null && i < propertyNames.length -1)
                throw new PropertyException("cannot get property '" + propertyName + "' - '" + name + "' is null");
            currentTarget = result;
        }

        return currentTarget;
    }


    /**
     * Set a direct property on the target object. Conversions from propertyValue to the proper destination type
     * are performed whenever possible.
     * @param target the target object on which to set the property.
     * @param propertyName the name of the property to set.
     * @param propertyValue the value of the property to set.
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    private static void setDirectProperty(Object target, String propertyName, Object propertyValue) throws IllegalAccessException, InvocationTargetException {
        Method setter = getSetter(target, propertyName);
        Class parameterType = setter.getParameterTypes()[0];
        if (propertyValue != null) {
            Object transformedPropertyValue = transform(propertyValue, parameterType);
            setter.invoke(target, new Object[] {transformedPropertyValue});
        }
        else {
            setter.invoke(target, new Object[] {null});
        }
    }

    /**
     * Build a map of direct javabeans properties of the target object.
     * @param target the target object from which to get properties names.
     * @return a Map of String with properties names as key and their values
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static Map getProperties(Object target) throws IllegalAccessException, InvocationTargetException {
        Map properties = new HashMap();
        Class clazz = target.getClass();
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String name = method.getName();
            if (method.getModifiers() == Modifier.PUBLIC && method.getParameterTypes().length == 0 && name.startsWith("get") && !name.equals("getClass")) {
                String propertyName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                Object propertyValue = method.invoke(target, (Object[]) null);

                if (propertyValue != null && propertyValue instanceof Properties) {
                    Map propertiesContent = getNestedProperties(propertyName, (Properties) propertyValue);
                    properties.putAll(propertiesContent);
                }
                else {
                    properties.put(propertyName, propertyValue);
                }
            } // if
        } // for
        return properties;
    }

    private static Map getNestedProperties(String prefix, Properties properties) {
        Map result = new HashMap();
        Iterator it = properties.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();
            result.put(prefix + '.' + name, value);
        }
        return result;
    }

    private static Object transform(Object value, Class destinationClass) {
        if (value.getClass() == destinationClass)
            return value;

        if ((destinationClass == int.class || destinationClass == Integer.class)  &&  value.getClass() == String.class) {
            return new Integer((String) value);
        }

        if ((destinationClass == boolean.class || destinationClass == Boolean.class)  &&  value.getClass() == String.class) {
            return new Boolean((String) value);
        }

        throw new PropertyException("cannot convert values of type '" + value.getClass().getName() + "' into type '" + destinationClass + "'");
    }

    private static void callSetter(Object target, String propertyName, Object parameter) throws IllegalAccessException, InvocationTargetException {
        Method setter = getSetter(target, propertyName);
        setter.invoke(target, new Object[] {parameter});
    }

    private static Object callGetter(Object target, String propertyName) throws IllegalAccessException, InvocationTargetException {
        Method getter = getGetter(target, propertyName);
        return getter.invoke(target, (Object[]) null);
    }

    private static Method getSetter(Object target, String propertyName) {
        if (propertyName == null || "".equals(propertyName))
            throw new PropertyException("encountered invalid null or empty property name");
        String setterName = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        Method[] methods = target.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals(setterName)  &&  method.getReturnType().equals(void.class)  &&  method.getParameterTypes().length == 1) {
                return method;
            }
        }
        throw new PropertyException("no writeable property '" + propertyName + "' in class '" + target.getClass().getName() + "'");
    }

    private static Method getGetter(Object target, String propertyName) {
        String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        Method[] methods = target.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals(getterName)  &&  !method.getReturnType().equals(void.class)  &&  method.getParameterTypes().length == 0) {
                return method;
            }
        }
        throw new PropertyException("no readable property '" + propertyName + "' in class '" + target.getClass().getName() + "'");
    }

    private static Class getPropertyType(Object target, String propertyName) {
        String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        Method[] methods = target.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals(getterName)  &&  !method.getReturnType().equals(void.class)  &&  method.getParameterTypes().length == 0) {
                return method.getReturnType();
            }
        }
        throw new PropertyException("no property '" + propertyName + "' in class '" + target.getClass().getName() + "'");
    }


}
