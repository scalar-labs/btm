package bitronix.tm.metric;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.utils.ClassLoaderUtils;

/**
 * MetricFactory - Factory for Metric instances.
 * <p/>
 * By default (the "auto" mode) it tries to locate the Codahale metric registry, and therefore the Instance will
 * reference a CodahaleMetricFactory instance.
 * <p/>
 * If you choose the "none" mode, you won;t get any MetricFactory.
 * <p/>
 * If you choose a custom factory class name, then you may use your own Metric implementation.
 *
 * @author Vlad Mihalcea
 */
public interface MetricFactory {

    /**
     * Instance resolving utility.
     */
    public static class Instance {

        /* Classes should use MetricFactory.INSTANCE to access the factory */
        static MetricFactory INSTANCE = Initializer.initialize(
                TransactionManagerServices.getConfiguration().getMetricFactoryClass());

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
        public static MetricFactory get() {
            return INSTANCE;
        }
    }

    /**
     * Get the domain related metric.
     *
     * @param domain domain
     * @return domain related metric
     */
    Metric metric(String domain);

    /**
     * Get a Class bound domain metric, embedding an uniqueName.
     *
     * @param clazz      class as the metric domain context
     * @param uniqueName unique name to ensure domain uniqueness
     * @return domain related metric
     */
    Metric metric(Class<?> clazz, String uniqueName);

    /**
     * Initializer class used to initialize the factory.
     */
    class Initializer {
        static MetricFactory initialize(String metricFactoryClass) {
            if (!"none".equals(metricFactoryClass)) {
                if ("auto".equals(metricFactoryClass)) {
                    try {
                        ClassLoaderUtils.loadClass("com.codahale.metrics.MetricRegistry");
                        return new CodahaleMetricFactory();
                    } catch (ClassNotFoundException cnfe) {
                        //DO NOTHING
                    }
                } else if (!metricFactoryClass.isEmpty()) {
                    try {
                        Class<?> clazz = ClassLoaderUtils.loadClass(metricFactoryClass);
                        return (MetricFactory) clazz.newInstance();
                    } catch (Exception ex) {
                        throw new BitronixRuntimeException("error initializing MetricFactory", ex);
                    }
                }
            }
            return null;
        }
    }
}
