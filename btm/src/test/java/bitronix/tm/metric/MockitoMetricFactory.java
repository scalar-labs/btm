package bitronix.tm.metric;

import org.mockito.Mockito;

/**
 * MockitoMetricFactory - MetricFactory for Mockito
 *
 * @author Vlad Mihalcea
 */
public class MockitoMetricFactory implements MetricFactory {

    private MetricFactory mockMetricFactory = Mockito.mock(MetricFactory.class);

    public MetricFactory getMockMetricFactory() {
        return mockMetricFactory;
    }

    public Metric metric(String domain) {
        return mockMetricFactory.metric(domain);
    }

    public Metric metric(Class<?> clazz, String uniqueName) {
        return mockMetricFactory.metric(clazz, uniqueName);
    }
}
