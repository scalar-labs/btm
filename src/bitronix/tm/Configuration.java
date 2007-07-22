package bitronix.tm;

import bitronix.tm.internal.InitializationException;
import bitronix.tm.internal.UidGenerator;
import bitronix.tm.internal.PropertyUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;
import java.util.Map;
import java.util.Iterator;
import java.lang.reflect.InvocationTargetException;

/**
 * Configuration repository of the transaction manager. You can either set parameters via the properties or the
 * configuration file. Once the transaction manager has started it is not possible to change the configuration.
 * <p>
 * The configuration filename must be specified with the <code>bitronix.tm.configuration</code> system property.
 * Here is the list of configuration properties:
 * <ul>
 *  <li><b>bitronix.tm.serverId -</b> <i>(defaults to server's IP address but that's unsafe)</i><br/>
 *      ID that must uniquely identify this TM instance.</li>
 *  <li><b>bitronix.tm.journal.disk.logPart1Filename -</b> <i>(defaults to btm1.tlog)</i><br/>
 *      Journal fragment file 1.</li>
 *  <li><b>bitronix.tm.journal.disk.logPart2Filename -</b> <i>(defaults to btm2.tlog)</i><br/>
 *      Journal fragment file 2.</li>
 *  <li><b>bitronix.tm.journal.disk.forcedWriteEnabled -</b> <i>(defaults to true)</i><br/>
 *      Are logs forced to disk ? Do not use in production since without disk force, integrity is not guaranteed.</li>
 *  <li><b>bitronix.tm.journal.disk.forceBatchingEnabled -</b> <i>(defaults to true)</i><br/>
 *      Are disk forces batched ? Disabling batching can seriously lower the transaction throughput.</li>
 *  <li><b>bitronix.tm.journal.disk.maxLogSize -</b> <i>(defaults to 2)</i><br/>
 *      Maximum size in megabytes of the journal fragments.</li>
 *  <li><b>bitronix.tm.journal.disk.filterLogStatus -</b> <i>(defaults to false)</i><br/>
 *      Should only mandatory logs be written ? Enabling this parameter lowers space usage of the fragments but makes
 *      debugging more complex.</li>
 *  <li><b>bitronix.tm.journal.disk.skipCorruptedLogs -</b> <i>(defaults to false)</i><br/>
 *      Should corrupted logs be skipped ? This is generally a bad idea unless you encounter corrupted logs issues
 *      and you want to restore as much as possible of of them.</li>
 *  <li><b>bitronix.tm.2pc.async -</b> <i>(defaults to false)</i><br/>
 *      Should two phase commit be executed asynchronously ? Asynchronous two phase commit can improve performance when
 *      there are many resources enlisted in transactions but is more CPU intensive due to the dynamic thread spawning
 *      requirements. It also makes debugging more complex.</li>
 *  <li><b>bitronix.tm.2pc.warnAboutZeroResourceTransactions -</b> <i>(defaults to true)</i><br/>
 *      Should zero-resource transactions result in a warning or not ?</li>
 *  <li><b>bitronix.tm.timer.defaultTransactionTimeout -</b> <i>(defaults to 60)</i><br/>
 *      Default transaction timeout in seconds.</li>
 *  <li><b>bitronix.tm.timer.transactionRetryInterval -</b> <i>(defaults to 10)</i><br/>
 *      Default pause interval in seconds after a resource communication error has been detected in a transaction
 *      before retry.</li>
 *  <li><b>bitronix.tm.timer.gracefulShutdownInterval -</b> <i>(defaults to 60)</i><br/>
 *      Maximum amount of seconds the TM will wait for transactions to get done before aborting them at shutdown time.</li>
 *  <li><b>bitronix.tm.timer.backgroundRecoveryInterval -</b> <i>(defaults to 0)</i><br/>Interval in minutes at which
 *      to run the recovery process on a timely basis in the background. Disabled when set to 0.</li>
 *  <li><b>bitronix.tm.resource.configuration -</b> <i>(optional)</i><br/>
 *      {@link bitronix.tm.resource.ResourceLoader} configuration file name.</li>
 * </ul>
 * The default settings are good enough for running in a test environment but certainly not for production usage.
 * </p>
 * <p>All those properties can refer to other defined ones or to system properties using the Ant notation:
 * <code>${some.property.name}</code>.</p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class Configuration {

    private final static Logger log = LoggerFactory.getLogger(Configuration.class);

    private String serverId;
    private byte[] serverIdArray;
    private String logPart1Filename;
    private String logPart2Filename;
    private boolean forcedWriteEnabled;
    private boolean forceBatchingEnabled;
    private int maxLogSizeInMb;
    private boolean filterLogStatus;
    private boolean skipCorruptedLogs;
    private boolean asynchronous2Pc;
    private boolean warnAboutZeroResourceTransaction;
    private int defaultTransactionTimeout;
    private int transactionRetryInterval;
    private int gracefulShutdownInterval;
    private int backgroundRecoveryInterval;
    private String resourceConfigurationFilename;


    protected Configuration() {
        try {
            InputStream in = null;
            Properties properties;
            try {
                String configurationFilename = System.getProperty("bitronix.tm.configuration");
                if (configurationFilename != null) {
                    log.info("loading configuration file " + configurationFilename);
                    in = new FileInputStream(configurationFilename);
                } else {
                    log.info("loading default configuration");
                    in = Thread.currentThread().getContextClassLoader().getResourceAsStream("bitronix-default-config.properties");
                }
                properties = new Properties();
                if (in != null)
                    properties.load(in);
                else
                    log.info("no configuration file found, using default settings");
            } finally {
                if (in != null) in.close();
            }

            serverId = getString(properties, "bitronix.tm.serverId", null);
            logPart1Filename = getString(properties, "bitronix.tm.journal.disk.logPart1Filename", "btm1.tlog");
            logPart2Filename = getString(properties, "bitronix.tm.journal.disk.logPart2Filename", "btm2.tlog");
            forcedWriteEnabled = getBoolean(properties, "bitronix.tm.journal.disk.forcedWriteEnabled", true);
            forceBatchingEnabled = getBoolean(properties, "bitronix.tm.journal.disk.forceBatchingEnabled", true);
            maxLogSizeInMb = getInt(properties, "bitronix.tm.journal.disk.maxLogSize", 2);
            filterLogStatus = getBoolean(properties, "bitronix.tm.journal.disk.filterLogStatus", false);
            skipCorruptedLogs = getBoolean(properties, "bitronix.tm.journal.disk.skipCorruptedLogs", false);
            asynchronous2Pc = getBoolean(properties, "bitronix.tm.2pc.async", false);
            warnAboutZeroResourceTransaction = getBoolean(properties, "bitronix.tm.2pc.warnAboutZeroResourceTransactions", true);
            defaultTransactionTimeout = getInt(properties, "bitronix.tm.timer.defaultTransactionTimeout", 60);
            transactionRetryInterval = getInt(properties, "bitronix.tm.timer.transactionRetryInterval", 10);
            gracefulShutdownInterval = getInt(properties, "bitronix.tm.timer.gracefulShutdownInterval", 60);
            backgroundRecoveryInterval = getInt(properties, "bitronix.tm.timer.backgroundRecoveryInterval", 0);
            resourceConfigurationFilename = getString(properties, "bitronix.tm.resource.configuration", null);
        } catch (IOException ex) {
            throw new InitializationException("error loading configuration", ex);
        }
    }


    /**
     * ASCII ID that must uniquely identify this TM instance. It must not exceed 51 characters or it will be truncated.
     * @return the unique ID of this TM instance.
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Set the ASCII ID that must uniquely identify this TM instance. It must not exceed 51 characters or it will be
     * truncated.
     * @param serverId the unique ID of this TM instance.
     */
    public void setServerId(String serverId) {
        checkNotStarted();
        this.serverId = serverId;
    }

    /**
     * Get the journal fragment file 1 name.
     * @return the journal fragment file 1 name.
     */
    public String getLogPart1Filename() {
        return logPart1Filename;
    }

    /**
     * Set the journal fragment file 1 name.
     * @param logPart1Filename the journal fragment file 1 name.
     */
    public void setLogPart1Filename(String logPart1Filename) {
        checkNotStarted();
        this.logPart1Filename = logPart1Filename;
    }

    /**
     * Get the journal fragment file 2 name.
     * @return the journal fragment file 2 name.
     */
    public String getLogPart2Filename() {
        return logPart2Filename;
    }

    /**
     * Set the journal fragment file 2 name.
     * @param logPart2Filename the journal fragment file 2 name.
     */
    public void setLogPart2Filename(String logPart2Filename) {
        checkNotStarted();
        this.logPart2Filename = logPart2Filename;
    }

    /**
     * Are logs forced to disk ?  Do not set to false in production since without disk force, integrity is not
     * guaranteed.
     * @return true if logs are forced to disk, false otherwise.
     */
    public boolean isForcedWriteEnabled() {
        return forcedWriteEnabled;
    }

    /**
     * Set if logs are forced to disk.  Do not set to false in production since without disk force, integrity is not
     * guaranteed.
     * @param forcedWriteEnabled true if logs should be forced to disk, false otherwise.
     */
    public void setForcedWriteEnabled(boolean forcedWriteEnabled) {
        checkNotStarted();
        this.forcedWriteEnabled = forcedWriteEnabled;
    }

    /**
     * Are disk forces batched ? Disabling batching can seriously lower the transaction manager's throughput.
     * @return true if disk forces are batched, false otherwise.
     */
    public boolean isForceBatchingEnabled() {
        return forceBatchingEnabled;
    }

    /**
     * Set if disk forces are batched. Disabling batching can seriously lower the transaction manager's throughput.
     * @param forceBatchingEnabled true if disk forces are batched, false otherwise.
     */
    public void setForceBatchingEnabled(boolean forceBatchingEnabled) {
        checkNotStarted();
        this.forceBatchingEnabled = forceBatchingEnabled;
    }

    /**
     * Maximum size in megabytes of the journal fragments. Larger logs allow transactions to stay longer in-doubt but
     * the TM pauses longer when a fragment is full.
     * @return the maximum size in megabytes of the journal fragments.
     */
    public int getMaxLogSizeInMb() {
        return maxLogSizeInMb;
    }

    /**
     * Set the Maximum size in megabytes of the journal fragments. Larger logs allow transactions to stay longer
     * in-doubt but the TM pauses longer when a fragment is full.
     * @param maxLogSizeInMb the maximum size in megabytes of the journal fragments.
     */
    public void setMaxLogSizeInMb(int maxLogSizeInMb) {
        checkNotStarted();
        this.maxLogSizeInMb = maxLogSizeInMb;
    }

    /**
     * Should only mandatory logs be written ? Enabling this parameter lowers space usage of the fragments but makes
     * debugging more complex.
     * @return true if only mandatory logs should be written.
     */
    public boolean isFilterLogStatus() {
        return filterLogStatus;
    }

    /**
     * Set if only mandatory logs should be written. Enabling this parameter lowers space usage of the fragments but
     * makes debugging more complex.
     * @param filterLogStatus true if only mandatory logs should be written.
     */
    public void setFilterLogStatus(boolean filterLogStatus) {
        checkNotStarted();
        this.filterLogStatus = filterLogStatus;
    }

    /**
     * Should corrupted logs be skipped ?
     * @return true if corrupted logs should be skipped.
     */
    public boolean isSkipCorruptedLogs() {
        return skipCorruptedLogs;
    }

    /**
     * Set if corrupted logs should be skipped.
     * @param skipCorruptedLogs true if corrupted logs should be skipped.
     */
    public void setSkipCorruptedLogs(boolean skipCorruptedLogs) {
        this.skipCorruptedLogs = skipCorruptedLogs;
    }

    /**
     * Should two phase commit be executed asynchronously ? Asynchronous two phase commit can improve performance when
     * there are many resources enlisted in transactions but is more CPU intensive due to the dynamic thread spawning
     * requirements. It also makes debugging more complex.
     * @return true if two phase commit should be executed asynchronously.
     */
    public boolean isAsynchronous2Pc() {
        return asynchronous2Pc;
    }

    /**
     * Set if two phase commit should be executed asynchronously. Asynchronous two phase commit can improve performance
     * when there are many resources enlisted in transactions but is more CPU intensive due to the dynamic thread
     * spawning requirements. It also makes debugging more complex.
     * @param asynchronous2Pc true if two phase commit should be executed asynchronously.
     */
    public void setAsynchronous2Pc(boolean asynchronous2Pc) {
        checkNotStarted();
        this.asynchronous2Pc = asynchronous2Pc;
    }

    /**
     * Should transactions executed without a single enlisted resource result in a warning or not ? Most of the time
     * transactions executed with no enlisted resource reflect a bug or a mis-configuration somewhere.
     * @return true if transactions executed without a single enlisted resource should result in a warning.
     */
    public boolean isWarnAboutZeroResourceTransaction() {
        return warnAboutZeroResourceTransaction;
    }

    /**
     * Set if transactions executed without a single enlisted resource should result in a warning or not. Most of the
     * time transactions executed with no enlisted resource reflect a bug or a mis-configuration somewhere.
     * @param warnAboutZeroResourceTransaction true if transactions executed without a single enlisted resource should
     *        result in a warning.
     */
    public void setWarnAboutZeroResourceTransaction(boolean warnAboutZeroResourceTransaction) {
        checkNotStarted();
        this.warnAboutZeroResourceTransaction = warnAboutZeroResourceTransaction;
    }

    /**
     * Default transaction timeout in seconds.
     * @return the default transaction timeout in seconds.
     */
    public int getDefaultTransactionTimeout() {
        return defaultTransactionTimeout;
    }

    /**
     * Set the default transaction timeout in seconds.
     * @param defaultTransactionTimeout the default transaction timeout in seconds.
     */
    public void setDefaultTransactionTimeout(int defaultTransactionTimeout) {
        checkNotStarted();
        this.defaultTransactionTimeout = defaultTransactionTimeout;
    }

    /**
     * Default pause interval in seconds after a resource communication error has been detected in a transaction before
     * retry.
     * @return the default pause interval in seconds.
     */
    public int getTransactionRetryInterval() {
        return transactionRetryInterval;
    }

    /**
     * Set the default pause interval in seconds after a resource communication error has been detected in a transaction
     * before retry.
     * @param transactionRetryInterval the default pause interval in seconds.
     */
    public void setTransactionRetryInterval(int transactionRetryInterval) {
        checkNotStarted();
        this.transactionRetryInterval = transactionRetryInterval;
    }

    /**
     * Maximum amount of seconds the TM will wait for transactions to get done before aborting them at shutdown time.
     * @return the maximum amount of time in seconds.
     */
    public int getGracefulShutdownInterval() {
        return gracefulShutdownInterval;
    }

    /**
     * Set the maximum amount of seconds the TM will wait for transactions to get done before aborting them at shutdown
     * time.
     * @param gracefulShutdownInterval the maximum amount of time in seconds.
     */
    public void setGracefulShutdownInterval(int gracefulShutdownInterval) {
        checkNotStarted();
        this.gracefulShutdownInterval = gracefulShutdownInterval;
    }

    /**
     * Interval in minutes at which to run the recovery process in the background. Disabled when set to 0.
     * @return the interval in minutes.
     */
    public int getBackgroundRecoveryInterval() {
        return backgroundRecoveryInterval;
    }

    /**
     * Set the interval in minutes at which to run the recovery process in the background. Disabled when set to 0.
     * @param backgroundRecoveryInterval the interval in minutes.
     */
    public void setBackgroundRecoveryInterval(int backgroundRecoveryInterval) {
        checkNotStarted();
        this.backgroundRecoveryInterval = backgroundRecoveryInterval;
    }

    /**
     * {@link bitronix.tm.resource.ResourceLoader} configuration file name.
     * @return the filename of the resources configuration file or null if not configured.
     */
    public String getResourceConfigurationFilename() {
        return resourceConfigurationFilename;
    }

    /**
     * Set the {@link bitronix.tm.resource.ResourceLoader} configuration file name.
     * @param resourceConfigurationFilename the filename of the resources configuration file or null you do not want to
     *        use the {@link bitronix.tm.resource.ResourceLoader}.
     */
    public void setResourceConfigurationFilename(String resourceConfigurationFilename) {
        checkNotStarted();
        this.resourceConfigurationFilename = resourceConfigurationFilename;
    }

    /**
     * Build the server ID byte array that will be prepended in generated UIDs. Once built, the value is cached for
     * the duration of the JVM lifespan.
     * @return the server ID.
     */
    public byte[] buildServerIdArray() {
        if (serverIdArray == null) {
            try {
                serverIdArray = serverId.substring(0, Math.min(serverId.length(), UidGenerator.MAX_SERVER_ID_LENGTH)).getBytes("US-ASCII");
            } catch (Exception ex) {
                log.warn("cannot get this JVM unique ID. Make sure it is configured and you only use ASCII characters. Will use IP address instead (unsafe for production usage!).");
                try {
                    serverIdArray = InetAddress.getLocalHost().getHostAddress().getBytes("US-ASCII");
                } catch (Exception ex2) {
                    final String unknownServerId = "unknown-server-id";
                    log.warn("cannot get the local IP address. Will replace it with '" + unknownServerId + "' constant (highly unsafe!).");
                    serverIdArray = unknownServerId.getBytes();
                }
            }
            log.info("JVM unique ID: <" + new String(serverIdArray) + ">");
        }
        return serverIdArray;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(512);
        sb.append("a Configuration with [");

        try {
            Map properties = PropertyUtils.getProperties(this);
            Iterator it = properties.keySet().iterator();
            while (it.hasNext()) {
                String property = (String) it.next();
                Object val = PropertyUtils.getProperty(this, property);
                sb.append(property);
                sb.append("=");
                sb.append(val);
                if (it.hasNext())
                sb.append(", ");
            }
        } catch (IllegalAccessException ex) {
            if (log.isDebugEnabled()) log.debug("error accessing properties of configuration object", ex);
        } catch (InvocationTargetException ex) {
            if (log.isDebugEnabled()) log.debug("error accessing properties of configuration object", ex);
        }

        sb.append("]");
        return sb.toString();
    }

    /*
    * Internal implementation
    */

    private void checkNotStarted() {
        if (TransactionManagerServices.isTransactionManagerRunning())
            throw new IllegalStateException("cannot change the configuration while the transaction manager is running");
    }

    static String getString(Properties properties, String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = properties.getProperty(key);
            if (value == null)
                return defaultValue;
        }
        return evaluate(properties, value);
    }

    static boolean getBoolean(Properties properties, String key, boolean defaultValue) {
        return Boolean.valueOf(getString(properties, key, "" + defaultValue)).booleanValue();
    }

    static int getInt(Properties properties, String key, int defaultValue) {
        return Integer.parseInt(getString(properties, key, "" + defaultValue));
    }

    private static String evaluate(Properties properties, String value) {
        String result = value;

        int startIndex = value.indexOf('$');
        if (startIndex > -1 && value.charAt(startIndex +1) == '{') {
            int endIndex = value.indexOf('}');
            if (startIndex +2 == endIndex)
                throw new IllegalArgumentException("property ref cannot refer to an empty name: ${}");
            if (endIndex == -1)
                throw new IllegalArgumentException("unclosed property ref: ${" + value.substring(startIndex +2));

            String subPropertyKey = value.substring(startIndex +2, endIndex);
            String subPropertyValue = getString(properties, subPropertyKey, null);

            result = result.substring(0, startIndex) + subPropertyValue + result.substring(endIndex +1);
            return evaluate(properties, result);
        }

        return result;
    }

}
