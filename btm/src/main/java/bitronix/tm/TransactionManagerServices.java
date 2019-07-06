/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm;

import java.util.Iterator;
import java.util.ServiceLoader;

import bitronix.tm.journal.Journal;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.resource.ResourceLoader;
import bitronix.tm.spi.BitronixContext;
import bitronix.tm.spi.DefaultBitronixContext;
import bitronix.tm.timer.TaskScheduler;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.utils.ExceptionAnalyzer;


/**
 * Container for all BTM services.
 * <p>The different services available are: {@link BitronixTransactionManager}, {@link BitronixTransactionSynchronizationRegistry}
 * {@link Configuration}, {@link Journal}, {@link TaskScheduler}, {@link ResourceLoader}, {@link Recoverer} and {@link Executor}.
 * They are used in all places of the TM so they must be globally reachable.</p>
 *
 * @author Ludovic Orban
 */
public class TransactionManagerServices {

    private final static BitronixContext context;

    static {
        Iterator<BitronixContext> iterator = ServiceLoader.load(BitronixContext.class).iterator();
        if (iterator.hasNext()) {
            context = iterator.next();
        } else {
            context = new DefaultBitronixContext();
        }
    }

    /**
     * Create an initialized transaction manager.
     * @return the transaction manager.
     */
    public static BitronixTransactionManager getTransactionManager() {
        return context.getTransactionManager();
    }

    /**
     * Create the JTA 1.1 TransactionSynchronizationRegistry.
     * @return the TransactionSynchronizationRegistry.
     */
    public static BitronixTransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return context.getTransactionSynchronizationRegistry();
    }

    /**
     * Create the configuration of all the components of the transaction manager.
     * @return the global configuration.
     */
    public static Configuration getConfiguration() {
        return context.getConfiguration();
    }

    /**
     * Create the transactions journal.
     * @return the transactions journal.
     */
    public static Journal getJournal() {
        return context.getJournal();
    }

    /**
     * Create the task scheduler.
     * @return the task scheduler.
     */
    public static TaskScheduler getTaskScheduler() {
        return context.getTaskScheduler();
    }

    /**
     * Create the resource loader.
     * @return the resource loader.
     */
    public static ResourceLoader getResourceLoader() {
        return context.getResourceLoader();
    }

    /**
     * Create the transaction recoverer.
     * @return the transaction recoverer.
     */
    public static Recoverer getRecoverer() {
        return context.getRecoverer();
    }

    /**
     * Create the 2PC executor.
     * @return the 2PC executor.
     */
    public static Executor getExecutor() {
        return context.getExecutor();
    }

    /**
     * Create the exception analyzer.
     * @return the exception analyzer.
     */
    public static ExceptionAnalyzer getExceptionAnalyzer() {
        return context.getExceptionAnalyzer();
    }

    /**
     * Check if the transaction manager has started.
     * @return true if the transaction manager has started.
     */
    public static boolean isTransactionManagerRunning() {
        return context.isTransactionManagerRunning();
    }

    /**
     * Check if the task scheduler has started.
     * @return true if the task scheduler has started.
     */
    public static boolean isTaskSchedulerRunning() {
        return context.isTaskSchedulerRunning();
    }

    /**
     * Clear services references. Called at the end of the shutdown procedure.
     */
    protected static synchronized void clear() {
        context.clear();
    }

}
