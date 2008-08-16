package bitronix.tm.internal;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional context of a thread. It contains both the active transaction (if any) and all default parameters
 * that a transaction running on a thread must inherit.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
        if (transaction == null)
            throw new IllegalArgumentException("transaction parameter cannot be null");
        if (log.isDebugEnabled()) log.debug("assigning <" + transaction + "> to <" + this + ">");
        this.transaction = transaction;
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
        if (timeout == 0) {
            int defaultValue = TransactionManagerServices.getConfiguration().getDefaultTransactionTimeout();
            if (log.isDebugEnabled()) log.debug("resetting default timeout of thread context to default value of " + defaultValue + "s");
            this.timeout = defaultValue;
        }
        else {    
            if (log.isDebugEnabled()) log.debug("changing default timeout of thread context to " + timeout + "s");
            this.timeout = timeout;
        }
    }

    /**
     * Return a human-readable representation.
     * @return a human-readable representation.
     */
    public String toString() {
        return "a ThreadContext with transaction " + transaction + ", default timeout " + timeout + "s";
    }
}
