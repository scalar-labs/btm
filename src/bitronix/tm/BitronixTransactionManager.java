package bitronix.tm;

import bitronix.tm.internal.*;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.timer.TaskScheduler;
import bitronix.tm.journal.Journal;
import bitronix.tm.twopc.executor.Executor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.transaction.*;
import javax.transaction.xa.XAException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Manifest;
import java.net.URL;

/**
 * Implementation of {@link TransactionManager} and {@link UserTransaction}.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixTransactionManager implements TransactionManager, UserTransaction, Referenceable {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransactionManager.class);

    private final static ThreadLocal threadLocalContexts = new ThreadLocal();
    private final static Map inFlightTransactions = Collections.synchronizedMap(new HashMap());
    private boolean shuttingDown;

    /**
     * Create the BitronixTransactionManager. Open the journal, load resources and start the recovery process
     * synchronously. If <code>bitronix.tm.timer.backgroundRecoveryInterval</code> is greater than 0, the recovery
     * service get scheduled for background recovery.
     */
    protected BitronixTransactionManager() {
        try {
            shuttingDown = false;
            logVersion();
            Configuration configuration = TransactionManagerServices.getConfiguration();
            configuration.buildServerIdArray(); // first call to initialize the ServerId

            if (log.isDebugEnabled()) log.debug("starting BitronixTransactionManager using " + configuration);
            TransactionManagerServices.getJournal().open();
            TransactionManagerServices.getResourceLoader().bindAll();
            TransactionManagerServices.getRecoverer().run();
            Exception completionException = TransactionManagerServices.getRecoverer().getCompletionException();
            if (completionException != null)
                throw new InitializationException("recovery failed, cannot safely start the transaction manager", completionException);

            int backgroundRecoveryInterval = TransactionManagerServices.getConfiguration().getBackgroundRecoveryInterval();
            if (backgroundRecoveryInterval > 0) {
                if (log.isDebugEnabled()) log.debug("recovery will run in the background every " + backgroundRecoveryInterval + " minutes");
                Date nextExecutionDate = new Date(System.currentTimeMillis() + (backgroundRecoveryInterval * 60L * 1000L));
                TransactionManagerServices.getTaskScheduler().scheduleRecovery(nextExecutionDate);
            }
        } catch (IOException ex) {
            throw new InitializationException("cannot open disk logger", ex);
        } catch (NamingException ex) {
            throw new InitializationException("cannot bind datasources", ex);
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

        currentTx.setActive();
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        BitronixTransaction currentTx = getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("committing transaction " + currentTx);
        if (currentTx == null)
            throw new IllegalStateException("no transaction started on this thread");

        try {
            currentTx.commit();
        } finally {
            clearCurrentContext(true);
        }
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        BitronixTransaction currentTx = getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("rolling back transaction " + currentTx);
        if (currentTx == null)
            throw new IllegalStateException("no transaction started on this thread");

        try {
            currentTx.rollback();
        } finally {
            clearCurrentContext(true);
        }
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
            clearCurrentContext(false);
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
                null,
                BitronixTransactionManagerFactory.class.getName(),
                null
        );
    }

    /**
     * Return all existing transactions.
     * @return a map of BitronixTransaction objects using Uid as key and BitronixTransaction as value.
     */
    public Map getInFlightTransactions() {
        synchronized (inFlightTransactions) {
            return new HashMap(inFlightTransactions);
        }
    }

    /**
     * Get the transaction currently registered on the current thread context.
     * @return the current transaction or null if no transaction has been started on the current thread.
     */
    public BitronixTransaction getCurrentTransaction() {
        if (threadLocalContexts.get() == null)
            return null;
        return getCurrentContext().getTransaction();
    }

    /**
     * Check if the transaction manager is in the process of shutting down.
     * @return true if the transaction manager is in the process of shutting down.
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Dump an overview of all running transactions as debug logs.
     */
    public void dumpTransactionContexts() {
        if (log.isDebugEnabled()) {
            if (log.isDebugEnabled()) log.debug("dumping " + inFlightTransactions.size() + " transaction context(s)");
            Iterator it = getInFlightTransactions().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                BitronixTransaction tx = (BitronixTransaction) entry.getValue();
                if (log.isDebugEnabled()) log.debug(tx.toString());
            }
        } // if
    }

    /**
     * Shut down the transaction manager and release all resources held by it.
     * <p>This includes the resources pools registered in the {@link bitronix.tm.resource.ResourceRegistrar} like JMS
     * and JDBC pools which automatically register themselves at creation time.</p><p>The Transaction Manager will wait
     * during a configurable graceful period before forcibly killing active transactions.</p>
     * After this method is called, attempts to create new transactions (via calls to
     * {@link javax.transaction.TransactionManager#begin()}) will be rejected with a {@link SystemException}.</p>
     * <p>This method is automatically called by a shutdown hook if not called manually by the application code.</p>
     * <p>Once the transaction manager has been shutdown, there is no way to restart it except by restarting the JVM.</p>
     * @see Runtime#addShutdownHook(Thread)
     * @see Configuration#getGracefulShutdownInterval()
     */
    public synchronized void shutdown() {
        if (isShuttingDown()) {
            if (log.isDebugEnabled()) log.debug("Transaction Manager is already shutting down or has shutdown");
            return;
        }

        log.info("shutting down Bitronix Transaction Manager");
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

        Set names = ResourceRegistrar.getResourcesUniqueNames();
        if (log.isDebugEnabled()) log.debug("closing all " + names.size() + " resource(s)");
        Iterator it = names.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            XAResourceProducer producer = ResourceRegistrar.get(name);
            if (log.isDebugEnabled()) log.debug("closing " + name + " - " + producer);
            try {
                producer.close();
            } catch (Exception ex) {
                log.warn("error closing resource " + producer, ex);
            }
        }

        Executor executor = TransactionManagerServices.getExecutor();
        if (log.isDebugEnabled()) log.debug("shutting down executor");
        executor.shutdown();

        TaskScheduler taskScheduler = TransactionManagerServices.getTaskScheduler();
        long gracefulShutdownTime = TransactionManagerServices.getConfiguration().getGracefulShutdownInterval() * 1000;
        try {
            if (log.isDebugEnabled()) log.debug("shutting down scheduler, graceful interval: " + gracefulShutdownTime + "ms");
            taskScheduler.setActive(false);
            taskScheduler.join(gracefulShutdownTime);
        } catch (InterruptedException ex) {
            log.error("could not stop the timer service within " + TransactionManagerServices.getConfiguration().getGracefulShutdownInterval() + "s");
        }

        Journal journal = TransactionManagerServices.getJournal();
        try {
            if (log.isDebugEnabled()) log.debug("closing disk journal");
            journal.close();
        } catch (IOException ex) {
            log.error("error shutting down disk journal. Transaction log integrity could be compromised !", ex);
        }

        // clear references
        TransactionManagerServices.clear();

        if (log.isDebugEnabled()) log.debug("shutdown ran successfully");
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
        BitronixTransaction transaction = new BitronixTransaction();
        Date timeoutDate = new Date(System.currentTimeMillis() + (getCurrentContext().getTimeout() * 1000L));
        TransactionManagerServices.getTaskScheduler().scheduleTransactionTimeout(transaction, timeoutDate);
        getCurrentContext().setTransaction(transaction);
        inFlightTransactions.put(transaction.getResourceManager().getGtrid(), transaction);
        return transaction;
    }

    /**
     * Unlink the transaction from the current thread's context.
     * @param unregisterTransaction true if you want to get rid of this transaction, false if you want to save it for
     *        later and just unlink it from the current context
     */
    private void clearCurrentContext(boolean unregisterTransaction) {
        if (log.isDebugEnabled()) log.debug("clearing current thread context: " + getCurrentContext());
        BitronixTransaction currentTransaction = getCurrentTransaction();
        if (currentTransaction != null && unregisterTransaction) {
            if (log.isDebugEnabled()) log.debug("unregistering in-flight transaction " + currentTransaction);
            inFlightTransactions.remove(currentTransaction.getResourceManager().getGtrid());
            TransactionManagerServices.getTaskScheduler().cancelTransactionTimeout(currentTransaction);
        }
        getCurrentContext().clear();
        if (log.isDebugEnabled()) log.debug("cleared current thread context: " + getCurrentContext());
    }

    /**
     * Bind a new context on the current thread.
     * @param context the context to bind.
     */
    private void setCurrentContext(ThreadContext context) {
        if (log.isDebugEnabled()) log.debug("changing current thread context to " + context);
        if (context == null) {
            log.error("setCurrentContext() should not be called with a null context, clearCurrentContext() should be used instead");
            clearCurrentContext(true);
        }
        threadLocalContexts.set(context);
    }

    /**
     * Get the context attached to the current thread. If there is no current context, a new one is created.
     * @return the context.
     */
    private ThreadContext getCurrentContext() {
        ThreadContext threadContext = (ThreadContext) threadLocalContexts.get();
        if (threadContext == null) {
            if (log.isDebugEnabled()) log.debug("creating new thread context");
            threadContext = new ThreadContext();
            setCurrentContext(threadContext);
        }
        return threadContext;
    }

}
