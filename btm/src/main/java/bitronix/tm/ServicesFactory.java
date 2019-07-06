package bitronix.tm;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Factory of services.
 */
public class ServicesFactory {
    private final static Logger log = LoggerFactory.getLogger(TransactionManagerServices.class);

    public static BitronixTransactionManager crateTransactionManager() {
        return new BitronixTransactionManager();
    }

    public static BitronixTransactionSynchronizationRegistry createTransactionSynchronizationRegistry() {
        return new BitronixTransactionSynchronizationRegistry();
    }

    public static Configuration createConfiguration() {
        return new Configuration();
    }

    public static Journal createJournal(Journal journal, String configuredJournal) throws InitializationException {
        if ("null".equals(configuredJournal) || null == configuredJournal) {
            journal = new NullJournal();
        } else if ("disk".equals(configuredJournal)) {
            journal = new DiskJournal();
        } else {
            try {
                Class<?> clazz = ClassLoaderUtils.loadClass(configuredJournal);
                journal = (Journal) clazz.newInstance();
            } catch (Exception ex) {
                throw new InitializationException("invalid journal implementation '" + configuredJournal + "'", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("using journal " + configuredJournal);
        }
        return journal;
    }

    public static TaskScheduler createTaskScheduler() {
        return new TaskScheduler();
    }

    public static ResourceLoader createResourceLoader() {
        return new ResourceLoader();
    }

    public static Recoverer createRecoverer() {
        return new Recoverer();
    }

    public static Executor createExecutor(boolean isAsynchronouse2Pc) {
        Executor executor;
        if (isAsynchronouse2Pc) {
            if (log.isDebugEnabled()) {
                log.debug("using AsyncExecutor");
            }
            executor = new AsyncExecutor();
        } else {
            if (log.isDebugEnabled()) {
                log.debug("using SyncExecutor");
            }
            executor = new SyncExecutor();
        }
        return executor;
    }

    public static ExceptionAnalyzer createExceptionAnalyser(String exceptionAnalyzerName) {
        ExceptionAnalyzer analyzer;
        analyzer = new DefaultExceptionAnalyzer();
        if (exceptionAnalyzerName != null) {
            try {
                analyzer = (ExceptionAnalyzer) ClassLoaderUtils.loadClass(exceptionAnalyzerName).newInstance();
            } catch (Exception ex) {
                log.warn("failed to initialize custom exception analyzer, using default one instead", ex);
            }
        }
        return analyzer;
    }


}
