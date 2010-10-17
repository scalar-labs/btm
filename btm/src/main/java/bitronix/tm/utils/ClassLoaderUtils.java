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

import java.io.InputStream;

/**
 * Static utility methods for loading classes and resources.
 */
public class ClassLoaderUtils {

    private final static Logger log = LoggerFactory.getLogger(ClassLoaderUtils.class);

    /**
     * Get the class loader which can be used to generate proxies without leaking memory.
     * @return the class loader which can be used to generate proxies without leaking memory.
     */
    public static ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return cl;
        }
        return ClassLoaderUtils.class.getClassLoader();
    }

    /**
     * Load a class by name. Tries the current thread's context loader then falls back to {@link Class#forName(String)}.
     * @param className name of the class to load.
     * @return the loaded class.
     * @throws ClassNotFoundException if the class cannot be found in the classpath.
     */
    public static Class loadClass(String className) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            try {
                return cl.loadClass(className);
            } catch (ClassNotFoundException ex) {
                if (log.isDebugEnabled()) log.debug("context classloader could not find class '" + className + "', trying Class.forName() instead");
            }
        }
        
        return Class.forName(className);
    }

    /**
     * Load a resource from the classpath. Tries the current thread's context loader then falls back to
     * {@link ClassLoader#getResourceAsStream(String)} using this class' classloader.
     * @param resourceName the resource name to load.
     * @return a {@link java.io.InputStream} if the resource could be found, null otherwise.
     */
    public static InputStream getResourceAsStream(String resourceName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null)
            return cl.getResourceAsStream(resourceName);

        return ClassLoaderUtils.class.getClassLoader().getResourceAsStream(resourceName);
    }
}