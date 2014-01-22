package bitronix.tm.metric;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Metric - This interface defines a metric basic behavior. This is required to isolate Bitronix from
 * external Metric dependencies.
 *
 * @author Vlad Mihalcea
 */
public interface Metric extends Serializable {

    /**
     * Each metric must define the unique domain it represents.
     *
     * @return metric domain
     */
    String getDomain();

    /**
     * Update the given histogram
     *
     * @param name  histogram name
     * @param value histogram instant value
     */
    void updateHistogram(String name, long value);

    /**
     * Update the given timer
     *
     * @param name  timer name
     * @param value timer instant value
     */
    void updateTimer(String name, long value, TimeUnit timeUnit);

    /**
     * Start metric reporting.
     */
    void start();

    /**
     * Stop metric reporting.
     */
    void stop();
}
