package bitronix.tm.spi;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.Configuration;
import bitronix.tm.ServicesFactory;
import bitronix.tm.journal.Journal;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.resource.ResourceLoader;
import bitronix.tm.timer.TaskScheduler;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.utils.ExceptionAnalyzer;

/**
 * Default context which keeps one instance of each service.
 */
public class DefaultBitronixContext implements BitronixContext {
    private final Lock transactionManagerLock = new ReentrantLock();
    private volatile BitronixTransactionManager transactionManager;

    private final AtomicReference<BitronixTransactionSynchronizationRegistry> transactionSynchronizationRegistryRef = new AtomicReference<BitronixTransactionSynchronizationRegistry>();
    private final AtomicReference<Configuration> configurationRef = new AtomicReference<Configuration>();
    private final AtomicReference<Journal> journalRef = new AtomicReference<Journal>();
    private final AtomicReference<TaskScheduler> taskSchedulerRef = new AtomicReference<TaskScheduler>();
    private final AtomicReference<ResourceLoader> resourceLoaderRef = new AtomicReference<ResourceLoader>();
    private final AtomicReference<Recoverer> recovererRef = new AtomicReference<Recoverer>();
    private final AtomicReference<Executor> executorRef = new AtomicReference<Executor>();
    private final AtomicReference<ExceptionAnalyzer> exceptionAnalyzerRef = new AtomicReference<ExceptionAnalyzer>();

    @Override
    public BitronixTransactionManager getTransactionManager() {
        transactionManagerLock.lock();
        try {
            if (transactionManager == null) {
                transactionManager = ServicesFactory.crateTransactionManager();
            }
            return transactionManager;
        } finally {
            transactionManagerLock.unlock();
        }
    }

    @Override
    public BitronixTransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry = transactionSynchronizationRegistryRef.get();
        if (transactionSynchronizationRegistry == null) {
            transactionSynchronizationRegistry = ServicesFactory.createTransactionSynchronizationRegistry();
            if (!transactionSynchronizationRegistryRef.compareAndSet(null, transactionSynchronizationRegistry)) {
                transactionSynchronizationRegistry = transactionSynchronizationRegistryRef.get();
            }
        }
        return transactionSynchronizationRegistry;
    }

    @Override
    public Configuration getConfiguration() {
        Configuration configuration = configurationRef.get();
        if (configuration == null) {
            configuration = ServicesFactory.createConfiguration();
            if (!configurationRef.compareAndSet(null, configuration)) {
                configuration = configurationRef.get();
            }
        }
        return configuration;
    }

    @Override
    public Journal getJournal() {
        Journal journal = journalRef.get();
        if (journal == null) {
            String configuredJournal = getConfiguration().getJournal();
            journal = ServicesFactory.createJournal(journal, configuredJournal);

            if (!journalRef.compareAndSet(null, journal)) {
                journal = journalRef.get();
            }
        }
        return journal;
    }

    @Override
    public TaskScheduler getTaskScheduler() {
        TaskScheduler taskScheduler = taskSchedulerRef.get();
        if (taskScheduler == null) {
            taskScheduler = ServicesFactory.createTaskScheduler();
            if (!taskSchedulerRef.compareAndSet(null, taskScheduler)) {
                taskScheduler = taskSchedulerRef.get();
            } else {
                taskScheduler.start();
            }
        }
        return taskScheduler;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        ResourceLoader resourceLoader = resourceLoaderRef.get();
        if (resourceLoader == null) {
            resourceLoader = ServicesFactory.createResourceLoader();
            if (!resourceLoaderRef.compareAndSet(null, resourceLoader)) {
                resourceLoader = resourceLoaderRef.get();
            }
        }
        return resourceLoader;
    }

    @Override
    public Recoverer getRecoverer() {
        Recoverer recoverer = recovererRef.get();
        if (recoverer == null) {
            recoverer = ServicesFactory.createRecoverer();
            if (!recovererRef.compareAndSet(null, recoverer)) {
                recoverer = recovererRef.get();
            }
        }
        return recoverer;
    }

    @Override
    public Executor getExecutor() {
        Executor executor = executorRef.get();
        if (executor == null) {
            executor = ServicesFactory.createExecutor(getConfiguration().isAsynchronous2Pc());
            if (!executorRef.compareAndSet(null, executor)) {
                executor.shutdown();
                executor = executorRef.get();
            }
        }
        return executor;
    }

    @Override
    public ExceptionAnalyzer getExceptionAnalyzer() {
        ExceptionAnalyzer analyzer = exceptionAnalyzerRef.get();
        if (analyzer == null) {
            String exceptionAnalyzerName = getConfiguration().getExceptionAnalyzer();
            analyzer = ServicesFactory.createExceptionAnalyser(exceptionAnalyzerName);
            if (!exceptionAnalyzerRef.compareAndSet(null, analyzer)) {
                analyzer.shutdown();
                analyzer = exceptionAnalyzerRef.get();
            }
        }
        return analyzer;
    }

    @Override
    public boolean isTransactionManagerRunning() {
        return transactionManager != null;
    }

    @Override
    public boolean isTaskSchedulerRunning() {
        return taskSchedulerRef.get() != null;
    }

    @Override
    public synchronized void clear() {
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
