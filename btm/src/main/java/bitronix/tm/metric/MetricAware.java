package bitronix.tm.metric;

/**
 * MetricAware - Interface to obtaining the current associated Metric
 *
 * @author Vlad Mihalcea
 */
public interface MetricAware {

    /**
     * Initialize and bind a metric to the current object.
     */
    void initializeMetric();

    /**
     * Get the current object metric.
     *
     * @return current object metric.
     */
    Metric getMetric();
}
