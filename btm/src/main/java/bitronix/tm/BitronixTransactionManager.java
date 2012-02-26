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

import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.ThreadContext;
import bitronix.tm.internal.XAResourceManager;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.InitializationException;
import bitronix.tm.utils.MonotonicClock;
import bitronix.tm.utils.Scheduler;
import bitronix.tm.utils.Service;
import bitronix.tm.utils.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link TransactionManager} and {@link UserTransaction}.
 *
 * @author lorban
 */
public class BitronixTransactionManager implements TransactionManager, UserTransaction, Referenceable, Service {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransactionManager.class);
    private final static String MDC_GTRID_KEY = "btm-gtrid";

    private final Map<Thread, ThreadContext> contexts = new ConcurrentHashMap<Thread, ThreadContext>(128, 0.75f, 128);
    private final Map<Uid, BitronixTransaction> inFlightTransactions = new ConcurrentHashMap<Uid, BitronixTransaction>(128, 0.75f, 128);

    private volatile boolean shuttingDown;

    /**
     * Create the {@link BitronixTransactionManager}. Open the journal, load resources and perform recovery
     * synchronously. The recovery service then gets scheduled for background recovery.
     */
    public BitronixTransactionManager() {
        try {
            shuttingDown = false;
            logVersion();
            Configuration configuration = TransactionManagerServices.getConfiguration();
            configuration.buildServerIdArray(); // first call will initialize the ServerId

            if (log.isDebugEnabled()) log.debug("starting BitronixTransactionManager using " + configuration);
            TransactionManagerServices.getJournal().open();
            TransactionManagerServices.getResourceLoader().init();
            TransactionManagerServices.getRecoverer().run();

            int backgroundRecoveryInterval = TransactionManagerServices.getConfiguration().getBackgroundRecoveryIntervalSeconds();
            if (backgroundRecoveryInterval < 1) {
                throw new InitializationException("invalid configuration value for backgroundRecoveryIntervalSeconds, found '" + backgroundRecoveryInterval + "' but it must be greater than 0");
            }

            if (log.isDebugEnabled()) log.debug("recovery will run in the background every " + backgroundRecoveryInterval + " second(s)");
            Date nextExecutionDate = new Date(MonotonicClock.currentTimeMillis() + (backgroundRecoveryInterval * 1000L));
            TransactionManagerServices.getTaskScheduler().scheduleRecovery(TransactionManagerServices.getRecoverer(), nextExecutionDate);
        } catch (IOException ex) {
            throw new InitializationException("cannot open disk journal", ex);
        } catch (Exception ex) {
            TransactionManagerServices.getJournal().shutdown();
            TransactionManagerServices.getResourceLoader().shutdown();
            throw new InitializationException("initialization failed, cannot safely start the transaction manager", ex);
        }
    }

    /**
     * Start a new transaction and bind the context to the calling thread.
     * @throws NotSupportedException if a transaction is already bound to the calling thread.
     * @throws SystemException if the transaction manager is shutting down.
     */
    public void begin() throws NotSupportedException, SystemException {
        if (log.isDebugEnabled()) log.debug("beginning a new transaction");
        if (isShuttingDown())
            throw new BitronixSystemException("cannot start a new transaction, transaction manager is shutting down");

        dumpTransactionContexts();

        BitronixTransaction currentTx = getCurrentTransaction();
        if (currentTx != null)
            throw new NotSupportedException("nested transactions not supported");
        currentTx = createTransaction();

        ClearContextSynchronization clearContextSynchronization = new ClearContextSynchronization(currentTx);
        try {
            currentTx.getSynchronizationScheduler().add(clearContextSynchronization, Scheduler.ALWAYS_LAST_POSITION -1);
            currentTx.setActive(getOrCreateCurrentContext().getTimeout());
            if (log.isDebugEnabled()) log.debug("begun new transaction at " + new Date(currentTx.getResourceManager().getGtrid().extractTimestamp()));
        } catch (RuntimeException ex) {
            clearContextSynchronization.afterCompletion(Status.STATUS_NO_TRANSACTION);
            throw ex;
        } catch (SystemException ex) {
            clearContextSynchronization.afterCompletion(Status.STATUS_NO_TRANSACTION);
            throw ex;
        }
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        BitronixTransaction currentTx = getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("committing transaction " + currentTx);
        if (currentTx == null)
            throw new IllegalStateException("no transaction started on this thread");

        currentTx.commit();
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        BitronixTransaction currentTx = getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("rolling back transaction " + currentTx);
        if (currentTx == null)
            throw new IllegalStateException("no transaction started on this thread");

        currentTx.rollback();
    }

    public int getStatus() throws SystemException {
        BitronixTransaction currentTx = getCurrentTransaction();
        if (currentTx == null)
           return Status.STATUS_NO_TRANSACTION;

        return currentTx.getStatus();
    }

    public Transaction getTransaction() throws SystemException {
        return getCurrentTransaction();
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        BitronixTransaction currentTx = getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("marking transaction as rollback only: " + currentTx);
        if (currentTx == null)
            throw new IllegalStateException("no transaction started on this thread");

        currentTx.setRollbackOnly();
    }

    public void setTransactionTimeout(int seconds) throws SystemException {
        if (seconds < 0)
            throw new BitronixSystemException("cannot set a timeout to less than 0 second (was: " + seconds + "s)");
        getOrCreateCurrentContext().setTimeout(seconds);
    }

    public Transaction suspend() throws SystemException {
        BitronixTransaction currentTx = getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("suspending transaction " + currentTx);
        if (currentTx == null)
            return null;

        try {
            currentTx.getResourceManager().suspend();
            clearCurrentContextForSuspension();
            return currentTx;
        } catch (XAException ex) {
            String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer().extractExtraXAExceptionDetails(ex);
            throw new BitronixSystemException("cannot suspend " + currentTx + ", error=" + Decoder.decodeXAExceptionErrorCode(ex) +
                    (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
        }
    }

    public void resume(Transaction transaction) throws InvalidTransactionException, IllegalStateException, SystemException {
        if (log.isDebugEnabled()) log.debug("resuming " + transaction);
        if (transaction == null)
            throw new InvalidTransactionException("resumed transaction cannot be null");
        if (!(transaction instanceof BitronixTransaction))
            throw new InvalidTransactionException("resumed transaction must be an instance of BitronixTransaction");

        BitronixTransaction tx = (BitronixTransaction) transaction;
        BitronixTransaction currentTx = getCurrentTransaction();
        if (currentTx != null)
            throw new IllegalStateException("a transaction is already running on this thread");

        try {
            XAResourceManager resourceManager = tx.getResourceManager();
            resourceManager.resume();
            ThreadContext ctx = new ThreadContext();
            ctx.setTransaction(tx);
            setCurrentContext(ctx);
        } catch (XAException ex) {
            String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer().extractExtraXAExceptionDetails(ex);
            throw new BitronixSystemException("cannot resume " + tx + ", error=" + Decoder.decodeXAExceptionErrorCode(ex) +
                    (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
        }
    }


    /**
     * BitronixTransactionManager can only have a single instance per JVM so this method always returns a reference
     * with no special information to find back the sole instance. BitronixTransactionManagerObjectFactory will be used
     * by the JNDI server to get the BitronixTransactionManager instance of the JVM.
     *
     * @return an empty reference to get the BitronixTransactionManager.
     */
    public Reference getReference() throws NamingException {
        return new Reference(
                BitronixTransactionManager.class.getName(),
                new StringRefAddr("TransactionManager", "BitronixTransactionManager"),
                BitronixTransactionManagerObjectFactory.class.getName(),
                null
        );
    }

    /**
     * Return all in-flight transactions.
     * @return a map of {@link BitronixTransaction} objects using {@link Uid} as key and {@link BitronixTransaction} as value.
     */
    public Map getInFlightTransactions() {
        return inFlightTransactions;
    }

    /**
     * Return the timestamp of the oldest in-flight transaction.
     * @return the timestamp or Long.MIN_VALUE if there is no in-flight transaction.
     */
    public long getOldestInFlightTransactionTimestamp() {
        if (inFlightTransactions.isEmpty()) {
            if (log.isDebugEnabled()) log.debug("oldest in-flight transaction's timestamp: " + Long.MIN_VALUE);
            return Long.MIN_VALUE;
        }

        long oldestTimestamp = Long.MAX_VALUE;

        for (Map.Entry<Uid, BitronixTransaction> entry : inFlightTransactions.entrySet()) {
            Uid gtrid = entry.getKey();
            long currentTimestamp = gtrid.extractTimestamp();

            if (currentTimestamp < oldestTimestamp)
                oldestTimestamp = currentTimestamp;
        }

        if (log.isDebugEnabled()) log.debug("oldest in-flight transaction's timestamp: " + oldestTimestamp);
        return oldestTimestamp;
    }

    /**
     * Get the transaction currently registered on the current thread context.
     * @return the current transaction or null if no transaction has been started on the current thread.
     */
    public BitronixTransaction getCurrentTransaction() {
        if (contexts.get(Thread.currentThread()) == null)
            return null;
        return getOrCreateCurrentContext().getTransaction();
    }

    /**
     * Check if the transaction manager is in the process of shutting down.
     * @return true if the transaction manager is in the process of shutting down.
     */
    private boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Dump an overview of all running transactions as debug logs.
     */
    public void dumpTransactionContexts() {
        if (log.isDebugEnabled()) {
            if (log.isDebugEnabled()) log.debug("dumping " + inFlightTransactions.size() + " transaction context(s)");
            for (Map.Entry<Uid, BitronixTransaction> entry : inFlightTransactions.entrySet()) {
                BitronixTransaction tx = entry.getValue();
                if (log.isDebugEnabled()) log.debug(tx.toString());
            }
        } // if
    }

    /**
     * Shut down the transaction manager and release all resources held by it.
     * <p>This call will also close the resources pools registered by the {@link bitronix.tm.resource.ResourceLoader}
     * like JMS and JDBC pools. The manually created ones are left untouched.</p>
     * <p>The Transaction Manager will wait during a configurable graceful period before forcibly killing active
     * transactions.</p>
     * After this method is called, attempts to create new transactions (via calls to
     * {@link javax.transaction.TransactionManager#begin()}) will be rejected with a {@link SystemException}.</p>
     * @see Configuration#getGracefulShutdownInterval()
     */
    public synchronized void shutdown() {
        if (isShuttingDown()) {
            if (log.isDebugEnabled()) log.debug("Transaction Manager has already shut down");
            return;
        }

        log.info("shutting down Bitronix Transaction Manager");
        internalShutdown();

        if (log.isDebugEnabled()) log.debug("shutting down resource loader");
        TransactionManagerServices.getResourceLoader().shutdown();

        if (log.isDebugEnabled()) log.debug("shutting down executor");
        TransactionManagerServices.getExecutor().shutdown();

        if (log.isDebugEnabled()) log.debug("shutting down task scheduler");
        TransactionManagerServices.getTaskScheduler().shutdown();

        if (log.isDebugEnabled()) log.debug("shutting down journal");
        TransactionManagerServices.getJournal().shutdown();

        if (log.isDebugEnabled()) log.debug("shutting down recoverer");
        TransactionManagerServices.getRecoverer().shutdown();

        if (log.isDebugEnabled()) log.debug("shutting down configuration");
        TransactionManagerServices.getConfiguration().shutdown();

        // clear references
        TransactionManagerServices.clear();

        if (log.isDebugEnabled()) log.debug("shutdown ran successfully");
    }

    private void internalShutdown() {
        shuttingDown = true;
        dumpTransactionContexts();

        int seconds = TransactionManagerServices.getConfiguration().getGracefulShutdownInterval();
        int txCount = 0;
        try {
            txCount = inFlightTransactions.size();
            while (seconds > 0  &&  txCount > 0) {
                if (log.isDebugEnabled()) log.debug("still " + txCount + " in-flight transactions, waiting... (" + seconds + " second(s) left)");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // ignore
                }
                seconds--;
                txCount = inFlightTransactions.size();
            }
        } catch (Exception ex) {
            log.error("cannot get a list of in-flight transactions", ex);
        }

        if (txCount > 0) {
            if (log.isDebugEnabled()) log.debug("still " + txCount + " in-flight transactions, shutting down anyway");
            dumpTransactionContexts();
        }
        else {
            if (log.isDebugEnabled()) log.debug("all transactions finished, resuming shutdown");
        }
    }

    public String toString() {
        return "a BitronixTransactionManager with " + inFlightTransactions.size() + " in-flight transaction(s)";
    }

    /*
    * Internal impl
    */

    /**
     * Output BTM version information as INFO log.
     */
    private void logVersion() {
        log.info("Bitronix Transaction Manager version " + Version.getVersion());
        if (log.isDebugEnabled()) log.debug("JVM version " + System.getProperty("java.version"));
    }

    /**
     * Create a new transaction on the current thread's context.
     * @return the created transaction.
     */
    private BitronixTransaction createTransaction() {
        BitronixTransaction transaction = new BitronixTransaction();
        getOrCreateCurrentContext().setTransaction(transaction);
        inFlightTransactions.put(transaction.getResourceManager().getGtrid(), transaction);
        MDC.put(MDC_GTRID_KEY, transaction.getGtrid());
        return transaction;
    }

    /**
     * Unlink the transaction from the current thread's context.
     */
    private void clearCurrentContextForSuspension() {
        if (log.isDebugEnabled()) log.debug("clearing current thread context: " + getOrCreateCurrentContext());
        contexts.remove(Thread.currentThread());
        if (log.isDebugEnabled()) log.debug("cleared current thread context: " + getOrCreateCurrentContext());
        MDC.remove(MDC_GTRID_KEY);
    }

    /**
     * Bind a new context on the current thread.
     * @param context the context to bind.
     */
    private void setCurrentContext(ThreadContext context) {
        if (log.isDebugEnabled()) log.debug("changing current thread context to " + context);
        if (context == null)
            throw new IllegalArgumentException("setCurrentContext() should not be called with a null context, clearCurrentContextForSuspension() should be used instead");
        contexts.put(Thread.currentThread(), context);
        if (context.getTransaction() != null) {
            MDC.put(MDC_GTRID_KEY, context.getTransaction().getGtrid());
        }
    }

    /**
     * Get the context attached to the current thread. If there is no current context, return null.
     * @return the context.
     */
    ThreadContext currentThreadContext() {
        return contexts.get(Thread.currentThread());
    }

    /**
     * Get the context attached to the current thread. If there is no current context, a new one is created.
     * @return the context.
     */
    private ThreadContext getOrCreateCurrentContext() {
        ThreadContext threadContext = contexts.get(Thread.currentThread());
        if (threadContext == null) {
            if (log.isDebugEnabled()) log.debug("creating new thread context");
            threadContext = new ThreadContext();
            setCurrentContext(threadContext);
        }
        return threadContext;
    }

    private class ClearContextSynchronization implements Synchronization {
        private final BitronixTransaction currentTx;

        public ClearContextSynchronization(BitronixTransaction currentTx) {
            this.currentTx = currentTx;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            Iterator<Map.Entry<Thread, ThreadContext>> it = contexts.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Thread, ThreadContext> entry = it.next();
                ThreadContext context = entry.getValue();
                if (context.getTransaction() == currentTx) {
                    if (log.isDebugEnabled()) log.debug("clearing thread context: " + context);
                    it.remove();
                    break;
                }
            }
            inFlightTransactions.remove(currentTx.getResourceManager().getGtrid());
            MDC.remove(MDC_GTRID_KEY);
        }

        public String toString() {
            return "a ClearContextSynchronization for " + currentTx;
        }
    }

}
