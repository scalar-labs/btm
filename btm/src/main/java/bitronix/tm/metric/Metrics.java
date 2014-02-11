package bitronix.tm.metric;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Metrics - This interface defines a metrics basic behavior. This is required to isolate Bitronix from
 * external Metrics dependencies.
 *
 * @author Vlad Mihalcea
 */
public interface Metrics extends Serializable {

    /**
     * Each metrics must define the unique domain it represents.
     *
     * @return metrics domain
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
     * Start metrics reporting.
     */
    void start();

    /**
     * Stop metrics reporting.
     */
    void stop();
}
