package bitronix.tm;

import bitronix.tm.internal.*;
import bitronix.tm.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.naming.*;
import javax.transaction.*;
import javax.transaction.xa.XAException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.Manifest;

/**
 * Implementation of {@link TransactionManager} and {@link UserTransaction}.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BitronixTransactionManager implements TransactionManager, UserTransaction, Referenceable, Service {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransactionManager.class);
    private final static String MDC_GTRID_KEY = "btm-gtrid";

    private final Map contexts = Collections.synchronizedMap(new HashMap());
    private final Map inFlightTransactions = Collections.synchronizedMap(new HashMap());
    private boolean shuttingDown;

    /**
     * Create the {@link BitronixTransactionManager}. Open the journal, load resources and perform recovery
     * synchronously. If <code>bitronix.tm.timer.backgroundRecoveryInterval</code> is greater than 0, the recovery
     * service gets scheduled for background recovery.
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
            Date nextExecutionDate = new Date(System.currentTimeMillis() + (backgroundRecoveryInterval * 1000L));
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

        currentTx.getSynchronizationScheduler().add(new ClearContextSynchronization(currentTx), Scheduler.ALWAYS_LAST_POSITION -1);
        currentTx.setActive();
        if (log.isDebugEnabled()) log.debug("begun new transaction at " + currentTx.getResourceManager().getGtrid().extractTimestamp());
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
        getCurrentContext().setTimeout(seconds);
    }

    public Transaction suspend() throws SystemException {
        BitronixTransaction currentTx = getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("suspending transaction " + currentTx);
        if (currentTx == null)
            return null;

        try {
            currentTx.getResourceManager().suspend();
            clearCurrentContext();
            return currentTx;
        } catch (XAException ex) {
            throw new BitronixSystemException("cannot suspend " + currentTx + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
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
            throw new BitronixSystemException("cannot resume " + tx + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
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
        synchronized (inFlightTransactions) {
            if (inFlightTransactions.size() == 0) {
                if (log.isDebugEnabled()) log.debug("oldest in-flight transaction's timestamp: " + Long.MIN_VALUE);
                return Long.MIN_VALUE;
            }

            long oldestTimestamp = Long.MAX_VALUE;

            Iterator it = inFlightTransactions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Uid gtrid = (Uid) entry.getKey();
                long currentTimestamp = gtrid.extractTimestamp();

                if (currentTimestamp < oldestTimestamp)
                    oldestTimestamp = currentTimestamp;
            }

            if (log.isDebugEnabled()) log.debug("oldest in-flight transaction's timestamp: " + oldestTimestamp);    
            return oldestTimestamp;
        }
    }

    /**
     * Get the transaction currently registered on the current thread context.
     * @return the current transaction or null if no transaction has been started on the current thread.
     */
    public BitronixTransaction getCurrentTransaction() {
        if (contexts.get(Thread.currentThread()) == null)
            return null;
        return getCurrentContext().getTransaction();
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
            synchronized (inFlightTransactions) {
                Iterator it = inFlightTransactions.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    BitronixTransaction tx = (BitronixTransaction) entry.getValue();
                    if (log.isDebugEnabled()) log.debug(tx.toString());
                }
            } // synchronized (inFlightTransactions)
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

        if (log.isDebugEnabled()) log.debug("shutting task scheduler");
        TransactionManagerServices.getTaskScheduler().shutdown();

        if (log.isDebugEnabled()) log.debug("shutting down disk journal");
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
        String message = "Bitronix Transaction Manager version ";
        try {
            String pathToThisClass = getClass().getResource("/" + getClass().getName().replace('.', '/') + ".class").toString();
            String manifestPath = pathToThisClass.substring(0, pathToThisClass.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            InputStream is = new URL(manifestPath).openStream();
            Manifest manifest = new Manifest(is);
            message += manifest.getMainAttributes().getValue("Implementation-Version");
            is.close();
        } catch (Exception ex) {
            message += "???";
        }
        log.info(message);
        if (log.isDebugEnabled()) log.debug("JVM version " + System.getProperty("java.version"));
    }

    /**
     * Create a new transaction on the current thread's context.
     * @return the created transaction.
     */
    private BitronixTransaction createTransaction() {
        BitronixTransaction transaction = new BitronixTransaction(getCurrentContext().getTimeout());
        getCurrentContext().setTransaction(transaction);
        inFlightTransactions.put(transaction.getResourceManager().getGtrid(), transaction);
        MDC.put(MDC_GTRID_KEY, transaction.getGtrid());
        return transaction;
    }

    /**
     * Unlink the transaction from the current thread's context.
     */
    private void clearCurrentContext() {
        if (log.isDebugEnabled()) log.debug("clearing current thread context: " + getCurrentContext());
        contexts.remove(Thread.currentThread());
        if (log.isDebugEnabled()) log.debug("cleared current thread context: " + getCurrentContext());
        MDC.remove(MDC_GTRID_KEY);
    }

    /**
     * Bind a new context on the current thread.
     * @param context the context to bind.
     */
    private void setCurrentContext(ThreadContext context) {
        if (log.isDebugEnabled()) log.debug("changing current thread context to " + context);
        if (context == null)
            throw new IllegalArgumentException("setCurrentContext() should not be called with a null context, clearCurrentContext() should be used instead");
        contexts.put(Thread.currentThread(), context);
        if (context.getTransaction() != null) {
            MDC.put(MDC_GTRID_KEY, context.getTransaction().getGtrid());
        }
    }

    /**
     * Get the context attached to the current thread. If there is no current context, a new one is created.
     * @return the context.
     */
    private ThreadContext getCurrentContext() {
        ThreadContext threadContext = (ThreadContext) contexts.get(Thread.currentThread());
        if (threadContext == null) {
            if (log.isDebugEnabled()) log.debug("creating new thread context");
            threadContext = new ThreadContext();
            setCurrentContext(threadContext);
        }
        return threadContext;
    }

    private class ClearContextSynchronization implements Synchronization {
        private BitronixTransaction currentTx;

        public ClearContextSynchronization(BitronixTransaction currentTx) {
            this.currentTx = currentTx;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            synchronized (contexts) {
                Iterator it = contexts.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    ThreadContext context = (ThreadContext) entry.getValue();
                    if (context.getTransaction() == currentTx) {
                        if (log.isDebugEnabled()) log.debug("clearing thread context: " + context);
                        it.remove();
                        break;
                    }
                } // while
            }
            inFlightTransactions.remove(currentTx.getResourceManager().getGtrid());
            MDC.remove(MDC_GTRID_KEY);
        }

        public String toString() {
            return "a ClearContextSynchronization for " + currentTx;
        }
    }

}
