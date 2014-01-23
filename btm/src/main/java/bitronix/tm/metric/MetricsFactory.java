package bitronix.tm.metric;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.utils.ClassLoaderUtils;

/**
 * MetricsFactory - Factory for Metrics instances.
 * <p/>
 * By default (the "auto" mode) it tries to locate the Codahale metrics registry, and therefore the Instance will
 * reference a CodahaleMetricsFactory instance.
 * <p/>
 * If you choose the "none" mode, you won;t get any MetricsFactory.
 * <p/>
 * If you choose a custom factory class name, then you may use your own Metrics implementation.
 *
 * @author Vlad Mihalcea
 */
public interface MetricsFactory {

    /**
     * Instance resolving utility.
     */
    public static class Instance {

        /* Classes should use MetricsFactory.INSTANCE to access the factory */
        static MetricsFactory INSTANCE = Initializer.initialize(
                TransactionManagerServices.getConfiguration().getMetricsFactoryClass());

        /**
         * Check if there is any initialized instance.
         *
         * @return is any instance available.
         */
        public static boolean exists() {
            return INSTANCE != null;
        }

        /**
         * Get the initialized instance.
         *
         * @return the initialized instance.
         */
        public static MetricsFactory get() {
            return INSTANCE;
        }
    }

    /**
     * Get the domain related metrics.
     *
     * @param domain domain
     * @return domain related metrics
     */
    Metrics metrics(String domain);

    /**
     * Get a Class bound domain metrics, embedding an uniqueName.
     *
     * @param clazz      class as the metrics domain context
     * @param uniqueName unique name to ensure domain uniqueness
     * @return domain related metrics
     */
    Metrics metrics(Class<?> clazz, String uniqueName);

    /**
     * Initializer class used to initialize the factory.
     */
    class Initializer {
        static MetricsFactory initialize(String metricFactoryClass) {
            if ("none".equals(metricFactoryClass)) {
                return null;
            }
            if ("auto".equals(metricFactoryClass)) {
                try {
                    ClassLoaderUtils.loadClass("com.codahale.metrics.MetricRegistry");
                    return new CodahaleMetricsFactory();
                } catch (ClassNotFoundException cnfe) {
                    //DO NOTHING
                }
            } else if (!metricFactoryClass.isEmpty()) {
                try {
                    Class<?> clazz = ClassLoaderUtils.loadClass(metricFactoryClass);
                    return (MetricsFactory) clazz.newInstance();
                } catch (Exception ex) {
                    throw new BitronixRuntimeException("error initializing MetricsFactory", ex);
                }
            }
            return null;
        }
    }
}
