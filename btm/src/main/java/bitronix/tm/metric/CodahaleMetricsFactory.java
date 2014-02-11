package bitronix.tm.metric;

/**
 * CodahaleMetricsFactory - com.codahale.metrics based MetricsFactory implementation.
 *
 * @author Vlad Mihalcea
 */
public class CodahaleMetricsFactory implements MetricsFactory {

    public Metrics metrics(String domain) {
        return new CodahaleMetrics(domain);
    }

    public Metrics metrics(Class<?> clazz, String uniqueName) {
        return new CodahaleMetrics(clazz, uniqueName);
    }
}
