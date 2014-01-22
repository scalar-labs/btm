package bitronix.tm.metric;

import bitronix.tm.TransactionManagerServices;

/**
 * MetricFactoryTestUtil - MetricFactory Test Utilities
 *
 * @author Vlad Mihalcea
 */
public class MetricFactoryTestUtil {

    /**
     * Initialize the MetricFactory instance with a different metricFactoryClass.
     *
     * @param metricFactoryClass MetricFactory class
     * @return MetricFactory
     */
    public static MetricFactory initialize(String metricFactoryClass) {
        MetricFactory.Instance.INSTANCE = MetricFactory.Initializer.initialize(
                metricFactoryClass);
        return MetricFactory.Instance.get();
    }

    /**
     * Initialize the MetricFactory instance with the default metricFactoryClass.
     *
     * @return MetricFactory
     */
    public static MetricFactory defaultInitialize() {
        MetricFactory.Instance.INSTANCE = MetricFactory.Initializer.initialize(
                TransactionManagerServices.getConfiguration().getMetricFactoryClass());
        return MetricFactory.Instance.get();
    }
}
