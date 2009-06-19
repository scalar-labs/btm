package bitronix.tm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Static utility methods for loading classes and resources.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 */
public class ClassLoaderUtils {

    private final static Logger log = LoggerFactory.getLogger(ClassLoaderUtils.class);


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