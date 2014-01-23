package bitronix.tm.metric;

import org.mockito.Mockito;

/**
 * MockitoMetricsFactory - MetricsFactory for Mockito
 *
 * @author Vlad Mihalcea
 */
public class MockitoMetricsFactory implements MetricsFactory {

    private MetricsFactory mockMetricsFactory = Mockito.mock(MetricsFactory.class);

    public MetricsFactory getMockMetricsFactory() {
        return mockMetricsFactory;
    }

    public Metrics metrics(String domain) {
        return mockMetricsFactory.metrics(domain);
    }

    public Metrics metrics(Class<?> clazz, String uniqueName) {
        return mockMetricsFactory.metrics(clazz, uniqueName);
    }
}
