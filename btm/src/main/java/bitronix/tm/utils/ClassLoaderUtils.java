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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Static utility methods for loading classes and resources.
 */
public class ClassLoaderUtils {

    private final static Logger log = LoggerFactory.getLogger(ClassLoaderUtils.class);

    public static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        for (Class<?> intf : Arrays.asList(clazz.getInterfaces())) {
            if (intf.getInterfaces().length > 0) {
                interfaces.addAll(getAllInterfaces(intf));
            }
            interfaces.add(intf);
        }
        if (clazz.getSuperclass() != null) {
            interfaces.addAll(getAllInterfaces(clazz.getSuperclass()));
        }

        if (clazz.isInterface()) {
            interfaces.add(clazz);
        }

        return interfaces;
    }

    /**
     * Get the class loader which can be used to generate proxies without leaking memory.
     * @return the class loader which can be used to generate proxies without leaking memory.
     */
    public static ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return new CascadingClassLoader(cl);
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
                return new CascadingClassLoader(cl).loadClass(className);
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

    private static class CascadingClassLoader extends ClassLoader {

        private ClassLoader contextLoader;

        CascadingClassLoader(ClassLoader contextLoader) {
            this.contextLoader = contextLoader;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                return contextLoader.loadClass(name);
            }
            catch (ClassNotFoundException cnfe) {
                return CascadingClassLoader.class.getClassLoader().loadClass(name);
            }
        }
    }
}
