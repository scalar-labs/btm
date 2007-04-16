package bitronix.tm.internal;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional context of a thread. It contains both the active transaction (if any) and all default parameters
 * that a transaction running on a thread must inherit.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class ThreadContext {

    private final static Logger log = LoggerFactory.getLogger(ThreadContext.class);

    private BitronixTransaction transaction;
    private int timeout = TransactionManagerServices.getConfiguration().getDefaultTransactionTimeout();

    /**
     * Return the transaction linked with this thread context.
     * @return the transaction linked to this thread context or null if there is none.
     */
    public BitronixTransaction getTransaction() {
        return transaction;
    }

    /**
     * Link a transaction with this thead context.
     * @param transaction the transaction to link.
     */
    public void setTransaction(BitronixTransaction transaction) {
        if (log.isDebugEnabled()) log.debug("assigning <" + transaction + "> to <" + this + ">");
        this.transaction = transaction;
    }

    /**
     * Clear this thread context. The attached resource manager and transaction are dropped.
     */
    public void clear() {
        if (log.isDebugEnabled()) log.debug("clearing " + this);
        transaction = null;
    }

    /**
     * Return this context's default timeout.
     * @return this context's default timeout.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Set this context's default timeout. All transactions started by the thread linked to this context will get
     * this value as their default timeout.
     * @param timeout the new default timeout value in seconds.
     */
    public void setTimeout(int timeout) {
        if (log.isDebugEnabled()) log.debug("changing default timeout of thread context to " + timeout + "s");
        this.timeout = timeout;
    }

    /**
     * Return a String representation.
     * @return a String representation.
     */
    public String toString() {
        return "a ThreadContext with transaction " + transaction + ", default timeout " + timeout + "s";
    }
}
