package bitronix.tm.metric;

import bitronix.tm.TransactionManagerServices;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * CodahaleMetrics - com.codahale.metrics based Metrics implementation.
 * <p/>
 * It may report the cummulated metrics to both the current log or platform JMX server.
 *
 * @author Vlad Mihalcea
 */
public class CodahaleMetrics implements Metrics {

    private final static Logger log = LoggerFactory.getLogger(CodahaleMetrics.class);

    private final String domain;

    private final MetricRegistry metricRegistry;
    private final Slf4jReporter logReporter;
    private final JmxReporter jmxReporter;

    public CodahaleMetrics(String domain) {
        this.domain = domain;
        this.metricRegistry = new MetricRegistry();
        this.logReporter = Slf4jReporter
                .forRegistry(metricRegistry)
                .outputTo(log)
                .build();
        if (!TransactionManagerServices.getConfiguration().isDisableJmx()) {
            jmxReporter = JmxReporter
                    .forRegistry(metricRegistry)
                    .inDomain(domain)
                    .build();
        } else {
            jmxReporter = null;
        }
    }

    public CodahaleMetrics(Class<?> clazz, String uniqueName) {
        this(MetricRegistry.name(clazz, uniqueName));
    }

    public String getDomain() {
        return domain;
    }

    public void updateHistogram(String name, long value) {
        metricRegistry.histogram(name).update(value);
    }

    public void updateTimer(String name, long value, TimeUnit timeUnit) {
        metricRegistry.timer(name).update(value, timeUnit);
    }

    public void start() {
        logReporter.start(5, TimeUnit.MINUTES);
        if (jmxReporter != null) {
            jmxReporter.start();
        }
    }

    public void stop() {
        logReporter.stop();
        if (jmxReporter != null) {
            jmxReporter.stop();
        }
    }
}
