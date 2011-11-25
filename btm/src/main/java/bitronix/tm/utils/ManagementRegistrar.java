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

import bitronix.tm.TransactionManagerServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Simple JMX facade. In case JMX is disabled, calling methods of this class have no effect.
 *
 * @author lorban
 */
public final class ManagementRegistrar {

    private final static Logger log = LoggerFactory.getLogger(ManagementRegistrar.class);


    private ManagementRegistrar() {
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
        MBeanServer mbeanServer = getMBeanServer();
        if (mbeanServer == null)
            return;

        try {
            mbeanServer.registerMBean(obj, new ObjectName(name));
        } catch (Exception ex) {
            log.warn("cannot register object with name " + name, ex);
        }
    }

    /**
     * Unregister the management object with the specified name.
     * @param name the name of the object.
     */
    public static void unregister(String name) {
        MBeanServer mbeanServer = getMBeanServer();
        if (mbeanServer == null)
            return;

        try {
            mbeanServer.unregisterMBean(new ObjectName(name));
        } catch (Exception ex) {
            log.warn("cannot unregister object with name " + name, ex);
        }
    }

    private static MBeanServer getMBeanServer() {
        if (!TransactionManagerServices.getConfiguration().isDisableJmx()) {
            return ManagementFactory.getPlatformMBeanServer();
        } else {
            return null;
        }
    }

}
