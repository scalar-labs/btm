package bitronix.tm.spi;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.Configuration;
import bitronix.tm.journal.Journal;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.resource.ResourceLoader;
import bitronix.tm.timer.TaskScheduler;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.utils.ExceptionAnalyzer;

/**
 * Keeps services instances.
 */
public interface BitronixContext {
    /**
     * Create an initialized transaction manager.
     * @return the transaction manager.
     */
    BitronixTransactionManager getTransactionManager();

    /**
     * Create the JTA 1.1 TransactionSynchronizationRegistry.
     * @return the TransactionSynchronizationRegistry.
     */
    BitronixTransactionSynchronizationRegistry getTransactionSynchronizationRegistry();

    /**
     * Create the configuration of all the components of the transaction manager.
     * @return the global configuration.
     */
    Configuration getConfiguration();

    /**
     * Create the transactions journal.
     * @return the transactions journal.
     */
    Journal getJournal();

    /**
     * Create the task scheduler.
     * @return the task scheduler.
     */
    TaskScheduler getTaskScheduler();

    /**
     * Create the resource loader.
     * @return the resource loader.
     */
    ResourceLoader getResourceLoader();

    /**
     * Create the transaction recoverer.
     * @return the transaction recoverer.
     */
    Recoverer getRecoverer();

    /**
     * Create the 2PC executor.
     * @return the 2PC executor.
     */
    Executor getExecutor();

    /**
     * Create the exception analyzer.
     * @return the exception analyzer.
     */
    ExceptionAnalyzer getExceptionAnalyzer();

    /**
     * Check if the transaction manager has started.
     * @return true if the transaction manager has started.
     */
    boolean isTransactionManagerRunning();

    /**
     * Check if the task scheduler has started.
     * @return true if the task scheduler has started.
     */
    boolean isTaskSchedulerRunning();

    /**
     * Clear services references. Called at the end of the shutdown procedure.
     */
    void clear();
}
