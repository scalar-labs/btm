package bitronix.tm.metric;

import junit.framework.TestCase;

/**
 * MetricsFactoryTest - MetricsFactory Test
 *
 * @author Vlad Mihalcea
 */
public class MetricsFactoryTest extends TestCase {

    public void testInitialize() {

        MetricsFactory metricsFactory;

        metricsFactory = MetricsFactoryTestUtil.defaultInitialize();
        assertEquals(CodahaleMetricsFactory.class, metricsFactory.getClass());
        assertTrue(MetricsFactory.Instance.exists());
        assertEquals(metricsFactory.getClass(), MetricsFactory.Instance.get().getClass());

        metricsFactory = MetricsFactoryTestUtil.initialize(MockitoMetricsFactory.class.getName());
        assertTrue(MetricsFactory.Instance.exists());
        assertEquals(MockitoMetricsFactory.class, metricsFactory.getClass());

        metricsFactory = MetricsFactoryTestUtil.initialize("none");
        assertNull(metricsFactory);
    }

    protected void tearDown() {
        MetricsFactoryTestUtil.defaultInitialize();
    }
}
