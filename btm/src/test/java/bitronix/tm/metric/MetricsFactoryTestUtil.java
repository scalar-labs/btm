package bitronix.tm.metric;

import bitronix.tm.TransactionManagerServices;

/**
 * MetricsFactoryTestUtil - MetricsFactory Test Utilities
 *
 * @author Vlad Mihalcea
 */
public class MetricsFactoryTestUtil {

    /**
     * Initialize the MetricsFactory instance with a different metricFactoryClass.
     *
     * @param metricFactoryClass MetricsFactory class
     * @return MetricsFactory
     */
    public static MetricsFactory initialize(String metricFactoryClass) {
        MetricsFactory.Instance.INSTANCE = MetricsFactory.Initializer.initialize(
                metricFactoryClass);
        return MetricsFactory.Instance.get();
    }

    /**
     * Initialize the MetricsFactory instance with the default metricFactoryClass.
     *
     * @return MetricsFactory
     */
    public static MetricsFactory defaultInitialize() {
        MetricsFactory.Instance.INSTANCE = MetricsFactory.Initializer.initialize(
                TransactionManagerServices.getConfiguration().getMetricsFactoryClass());
        return MetricsFactory.Instance.get();
    }
}
