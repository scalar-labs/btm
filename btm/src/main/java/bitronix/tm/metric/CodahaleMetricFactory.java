package bitronix.tm.metric;

/**
 * CodahaleMetricFactory - com.codahale.metrics based MetricFactory implementation.
 *
 * @author Vlad Mihalcea
 */
public class CodahaleMetricFactory implements MetricFactory {

    public Metric metric(String domain) {
        return new CodahaleMetric(domain);
    }

    public Metric metric(Class<?> clazz, String uniqueName) {
        return new CodahaleMetric(clazz, uniqueName);
    }
}
