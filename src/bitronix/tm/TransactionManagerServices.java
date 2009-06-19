package bitronix.tm;

import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.NullJournal;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.resource.ResourceLoader;
import bitronix.tm.timer.TaskScheduler;
import bitronix.tm.twopc.executor.*;
import bitronix.tm.utils.InitializationException;
import bitronix.tm.utils.ClassLoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for all BTM services.
 * <p>The different services available are: {@link BitronixTransactionManager}, {@link Configuration}, {@link Journal},
 * {@link TaskScheduler}, {@link ResourceLoader}, {@link Recoverer} and {@link Executor}. They are used in all places
 * of the TM so they must be globally reachable.</p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TransactionManagerServices {

    private final static Logger log = LoggerFactory.getLogger(TransactionManagerServices.class);

    private static BitronixTransactionManager transactionManager;
    private static Configuration configuration;
    private static Journal journal;
    private static TaskScheduler taskScheduler;
    private static ResourceLoader resourceLoader;
    private static Recoverer recoverer;
    private static Executor executor;

    /**
     * Create an initialized transaction manager.
     * @return the transaction manager.
     */
    public synchronized static BitronixTransactionManager getTransactionManager() {
        if (transactionManager == null)
            transactionManager = new BitronixTransactionManager();
        return transactionManager;
    }

    /**
     * Create the configuration of all the components of the transaction manager.
     * @return the global configuration.
     */
    public synchronized static Configuration getConfiguration() {
        if (configuration == null)
            configuration = new Configuration();
        return configuration;
    }

    /**
     * Create the transactions journal.
     * @return the transactions journal.
     */
    public synchronized static Journal getJournal() {
        if (journal == null) {
            String configuredJounal = getConfiguration().getJournal();
            if ("disk".equals(configuredJounal))
                journal = new DiskJournal();
            else if ("null".equals(configuredJounal))
                journal = new NullJournal();
            else {
                try {
                    Class clazz = ClassLoaderUtils.loadClass(configuredJounal);
                    journal = (Journal) clazz.newInstance();
                } catch (Exception ex) {
                    throw new InitializationException("invalid journal implementation '" + configuredJounal + "'", ex);
                }
            }
            if (log.isDebugEnabled()) log.debug("using journal " + configuredJounal);
        }
        return journal;
    }

    /**
     * Create the task scheduler.
     * @return the task scheduler.
     */
    public synchronized static TaskScheduler getTaskScheduler() {
        if (taskScheduler == null) {
            taskScheduler = new TaskScheduler();
            taskScheduler.start();
        }
        return taskScheduler;
    }

    /**
     * Create the resource loader.
     * @return the resource loader.
     */
    public synchronized static ResourceLoader getResourceLoader() {
        if (resourceLoader == null) {
            resourceLoader = new ResourceLoader();
        }
        return resourceLoader;
    }

    /**
     * Create the transaction recoverer.
     * @return the transaction recoverer.
     */
    public synchronized static Recoverer getRecoverer() {
        if (recoverer == null) {
            recoverer = new Recoverer();
        }
        return recoverer;
    }

    /**
     * Create the 2PC executor.
     * @return the 2PC executor.
     */
    public synchronized static Executor getExecutor() {
        if (executor == null) {
            boolean async = getConfiguration().isAsynchronous2Pc();
            if (async) {
                if (log.isDebugEnabled()) log.debug("trying to use ConcurrentExecutor");
                executor = new ConcurrentExecutor();
                if (!executor.isUsable()) {
                    if (log.isDebugEnabled()) log.debug("trying to use BackportConcurrentExecutor");
                    executor = new BackportConcurrentExecutor();
                }
                if (!executor.isUsable()) {
                    if (log.isDebugEnabled()) log.debug("using SimpleAsyncExecutor");
                    executor = new SimpleAsyncExecutor();
                }
            }
            else {
                if (log.isDebugEnabled()) log.debug("using SyncExecutor");
                executor = new SyncExecutor();
            }
        }
        return executor;
    }

    /**
     * Check if the transaction manager has started.
     * @return true if the transaction manager has started.
     */
    public synchronized static boolean isTransactionManagerRunning() {
        return transactionManager != null;
    }

    /**
     * Check if the task scheduler has started.
     * @return true if the task scheduler has started.
     */
    public synchronized static boolean isTaskSchedulerRunning() {
        return taskScheduler != null;
    }

    /**
     * Clear services references. Called at the end of the shutdown procedure.
     */
    protected static synchronized void clear() {
        transactionManager = null;
        configuration = null;
        journal = null;
        taskScheduler = null;
        resourceLoader = null;
        recoverer = null;
        executor = null;
    }

}
