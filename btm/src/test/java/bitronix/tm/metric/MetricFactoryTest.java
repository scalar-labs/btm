package bitronix.tm.metric;

import junit.framework.TestCase;

/**
 * MetricFactoryTest - MetricFactory Test
 *
 * @author Vlad Mihalcea
 */
public class MetricFactoryTest extends TestCase {

    public void testInitialize() {

        MetricFactory metricFactory;

        metricFactory = MetricFactoryTestUtil.defaultInitialize();
        assertEquals(CodahaleMetricFactory.class, metricFactory.getClass());
        assertTrue(MetricFactory.Instance.exists());
        assertEquals(metricFactory.getClass(), MetricFactory.Instance.get().getClass());

        metricFactory = MetricFactoryTestUtil.initialize(MockitoMetricFactory.class.getName());
        assertTrue(MetricFactory.Instance.exists());
        assertEquals(MockitoMetricFactory.class, metricFactory.getClass());

        metricFactory = MetricFactoryTestUtil.initialize("none");
        assertNull(metricFactory);
    }

    protected void tearDown() {
        MetricFactoryTestUtil.defaultInitialize();
    }
}
