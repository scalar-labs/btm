/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm;

import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.NullJournal;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.resource.ResourceLoader;
import bitronix.tm.timer.TaskScheduler;
import bitronix.tm.twopc.executor.AsyncExecutor;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.twopc.executor.SyncExecutor;
import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.DefaultExceptionAnalyzer;
import bitronix.tm.utils.ExceptionAnalyzer;
import bitronix.tm.utils.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Container for all BTM services.
 * <p>The different services available are: {@link BitronixTransactionManager}, {@link BitronixTransactionSynchronizationRegistry}
 * {@link Configuration}, {@link Journal}, {@link TaskScheduler}, {@link ResourceLoader}, {@link Recoverer} and {@link Executor}.
 * They are used in all places of the TM so they must be globally reachable.</p>
 *
 * @author lorban
 */
public class TransactionManagerServices {

    private final static Logger log = LoggerFactory.getLogger(TransactionManagerServices.class);

    private static final Lock transactionManagerLock = new ReentrantLock();
    private static volatile BitronixTransactionManager transactionManager;

    private static final AtomicReference<BitronixTransactionSynchronizationRegistry> transactionSynchronizationRegistryRef = new AtomicReference<BitronixTransactionSynchronizationRegistry>();
    private static final AtomicReference<Configuration> configurationRef = new AtomicReference<Configuration>();
    private static final AtomicReference<Journal> journalRef = new AtomicReference<Journal>();
    private static final AtomicReference<TaskScheduler> taskSchedulerRef = new AtomicReference<TaskScheduler>();
    private static final AtomicReference<ResourceLoader> resourceLoaderRef = new AtomicReference<ResourceLoader>();
    private static final AtomicReference<Recoverer> recovererRef = new AtomicReference<Recoverer>();
    private static final AtomicReference<Executor> executorRef = new AtomicReference<Executor>();
    private static final AtomicReference<ExceptionAnalyzer> exceptionAnalyzerRef = new AtomicReference<ExceptionAnalyzer>();

    /**
     * Create an initialized transaction manager.
     * @return the transaction manager.
     */
    public static BitronixTransactionManager getTransactionManager() {
        transactionManagerLock.lock();
        try {
            if (transactionManager == null) {
                transactionManager = new BitronixTransactionManager();
            }
            return transactionManager;
        } finally {
            transactionManagerLock.unlock();
        }
    }

    /**
     * Create the JTA 1.1 TransactionSynchronizationRegistry.
     * @return the TransactionSynchronizationRegistry.
     */
    public static BitronixTransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry = transactionSynchronizationRegistryRef.get();
        if (transactionSynchronizationRegistry == null) {
            transactionSynchronizationRegistry = new BitronixTransactionSynchronizationRegistry();
            if (!transactionSynchronizationRegistryRef.compareAndSet(null, transactionSynchronizationRegistry)) {
                transactionSynchronizationRegistry = transactionSynchronizationRegistryRef.get();
            }
        }
        return transactionSynchronizationRegistry;
    }

    /**
     * Create the configuration of all the components of the transaction manager.
     * @return the global configuration.
     */
    public static Configuration getConfiguration() {
        Configuration configuration = configurationRef.get();
        if (configuration == null) {
            configuration = new Configuration();
            if (!configurationRef.compareAndSet(null, configuration)) {
                configuration = configurationRef.get();
            }
        }
        return configuration;
    }

    /**
     * Create the transactions journal.
     * @return the transactions journal.
     */
    public static Journal getJournal() {
        Journal journal = journalRef.get();
        if (journal == null) {
            String configuredJournal = getConfiguration().getJournal();
            if ("null".equals(configuredJournal) || null == configuredJournal) {
                journal = new NullJournal();
            } else if ("disk".equals(configuredJournal)) {
                journal = new DiskJournal();
            } else {
                try {
                    Class clazz = ClassLoaderUtils.loadClass(configuredJournal);
                    journal = (Journal) clazz.newInstance();
                } catch (Exception ex) {
                    throw new InitializationException("invalid journal implementation '" + configuredJournal + "'", ex);
                }
            }
            if (log.isDebugEnabled()) log.debug("using journal " + configuredJournal);

            if (!journalRef.compareAndSet(null, journal)) {
                journal = journalRef.get();
            }
        }
        return journal;
    }

    /**
     * Create the task scheduler.
     * @return the task scheduler.
     */
    public static TaskScheduler getTaskScheduler() {
        TaskScheduler taskScheduler = taskSchedulerRef.get();
        if (taskScheduler == null) {
            taskScheduler = new TaskScheduler();
            if (!taskSchedulerRef.compareAndSet(null, taskScheduler)) {
                taskScheduler = taskSchedulerRef.get();
            } else {
                taskScheduler.start();
            }
        }
        return taskScheduler;
    }

    /**
     * Create the resource loader.
     * @return the resource loader.
     */
    public static ResourceLoader getResourceLoader() {
        ResourceLoader resourceLoader = resourceLoaderRef.get();
        if (resourceLoader == null) {
            resourceLoader = new ResourceLoader();
            if (!resourceLoaderRef.compareAndSet(null, resourceLoader)) {
                resourceLoader = resourceLoaderRef.get();
            }
        }        
        return resourceLoader;
    }

    /**
     * Create the transaction recoverer.
     * @return the transaction recoverer.
     */
    public static Recoverer getRecoverer() {
        Recoverer recoverer = recovererRef.get();
        if (recoverer == null) {
            recoverer = new Recoverer();
            if (!recovererRef.compareAndSet(null, recoverer)) {
                recoverer = recovererRef.get();
            }
        }
        return recoverer;
    }

    /**
     * Create the 2PC executor.
     * @return the 2PC executor.
     */
    public static Executor getExecutor() {
        Executor executor = executorRef.get();
        if (executor == null) {
            if (getConfiguration().isAsynchronous2Pc()) {
                if (log.isDebugEnabled()) log.debug("using AsyncExecutor");
                executor = new AsyncExecutor();
            } else {
                if (log.isDebugEnabled()) log.debug("using SyncExecutor");
                executor = new SyncExecutor();
            }
            if (!executorRef.compareAndSet(null, executor)) {
                executor.shutdown();
                executor = executorRef.get();
            }
        }
        return executor;
    }

    /**
     * Create the exception analyzer.
     * @return the exception analyzer.
     */
   public static ExceptionAnalyzer getExceptionAnalyzer() {
        ExceptionAnalyzer analyzer = exceptionAnalyzerRef.get();
        if (analyzer == null) {
            String exceptionAnalyzerName = getConfiguration().getExceptionAnalyzer();
            analyzer = new DefaultExceptionAnalyzer();
            if (exceptionAnalyzerName != null) {
                try {
                    analyzer = (ExceptionAnalyzer) ClassLoaderUtils.loadClass(exceptionAnalyzerName).newInstance();
                } catch (Exception ex) {
                    log.warn("failed to initialize custom exception analyzer, using default one instead", ex);
                }
            }
            if (!exceptionAnalyzerRef.compareAndSet(null, analyzer)) {
                analyzer.shutdown();
                analyzer = exceptionAnalyzerRef.get();
            }
        }
        return analyzer;
    }

    /**
     * Check if the transaction manager has started.
     * @return true if the transaction manager has started.
     */
    public static boolean isTransactionManagerRunning() {
        return transactionManager != null;
    }

    /**
     * Check if the task scheduler has started.
     * @return true if the task scheduler has started.
     */
    public static boolean isTaskSchedulerRunning() {
        return taskSchedulerRef.get() != null;
    }

    /**
     * Clear services references. Called at the end of the shutdown procedure.
     */
    protected static synchronized void clear() {
        transactionManager = null;

        transactionSynchronizationRegistryRef.set(null);
        configurationRef.set(null);
        journalRef.set(null);
        taskSchedulerRef.set(null);
        resourceLoaderRef.set(null);
        recovererRef.set(null);
        executorRef.set(null);
        exceptionAnalyzerRef.set(null);
    }

}
