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
package bitronix.tm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.TransactionManagerServices;

/**
 * Simple JMX facade. In case there is no JMX implementation available, calling methods of this class have no effect.
 *
 * @author lorban
 */
public class ManagementRegistrar {

    private final static Logger log = LoggerFactory.getLogger(ManagementRegistrar.class);
    private static Object mbeanServer;
    private static Method registerMBeanMethod;
    private static Method unregisterMBeanMethod;
    private static Constructor objectNameConstructor;

    static {
        boolean enableJmx = !TransactionManagerServices.getConfiguration().isDisableJmx();

        if (enableJmx) {
            try {
                Class managementFactoryClass = ClassLoaderUtils.loadClass("java.lang.management.ManagementFactory");
                Method getPlatformMBeanServerMethod = managementFactoryClass.getMethod("getPlatformMBeanServer", (Class[]) null);
                mbeanServer = getPlatformMBeanServerMethod.invoke(managementFactoryClass, (Object[]) null);

                Class objectNameClass = ClassLoaderUtils.loadClass("javax.management.ObjectName");
                objectNameConstructor = objectNameClass.getConstructor(new Class[] {String.class});

                registerMBeanMethod = mbeanServer.getClass().getMethod("registerMBean", new Class[] {Object.class, objectNameClass});
                unregisterMBeanMethod = mbeanServer.getClass().getMethod("unregisterMBean", new Class[] {objectNameClass});
            } catch (Exception ex) {
                // no management in case an exception is thrown
                mbeanServer = null;
            }
        } // if (enableJmx)
    }

    /**
     * Replace characters considered illegal in a management object's name.
     * @param name the name to work on.
     * @return a fully valid name where all invalid characters have been replaced with '_'.
     */
    public static String makeValidName(String name) {
        return name.replaceAll("[\\:\\,\\=,\\.]", "_");
    }

    /**
     * Register the specified management object.
     * @param name the name of the object.
     * @param obj the management object.
     */
    public static void register(String name, Object obj) {
        if (mbeanServer == null)
            return;

        try {
            Object objName = buildObjectName(name);
            mbeanServerCall(registerMBeanMethod, new Object[] {obj, objName});
        } catch (Exception ex) {
            log.warn("cannot register object with name " + name, ex);
        }
    }

    /**
     * Unregister the management object with the specified name.
     * @param name the name of the object.
     */
    public static void unregister(String name) {
        if (mbeanServer == null)
            return;

        try {
            Object objName = buildObjectName(name);
            mbeanServerCall(unregisterMBeanMethod, new Object[] {objName});
        } catch (Exception ex) {
            log.warn("cannot unregister object with name " + name, ex);
        }
    }


    /* internal impl */

    private static Object buildObjectName(String name) {
        try {
            return objectNameConstructor.newInstance(new Object[] {name});
        } catch (Exception ex) {
            throw new BitronixRuntimeException("cannot build ObjectName with name=" + name, ex);
        }
    }

    private static void mbeanServerCall(Method method, Object[] params) {
        try {
            method.invoke(mbeanServer, params);
        } catch (Exception ex) {
            throw new BitronixRuntimeException("cannot call method '" + method.getName() + "'", ex);
        }
    }

}
