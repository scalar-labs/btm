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
package bitronix.tm.internal;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Transactional context of a thread. It contains both the active transaction (if any) and all default parameters
 * that a transaction running on a thread must inherit.
 *
 * @author lorban
 */
public class ThreadContext {

    private final static Logger log = LoggerFactory.getLogger(ThreadContext.class);

    private volatile BitronixTransaction transaction;
    private volatile int timeout = TransactionManagerServices.getConfiguration().getDefaultTransactionTimeout();
    private final Map<Object, Object> resources = Collections.synchronizedMap(new HashMap<Object, Object>());

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
     * Get this context's resources, in the JTA 1.1 TransactionSynchronizationRegistry sense.
     * @return this context's resources.
     */
    public Map<Object, Object> getResources() {
        return resources;
    }

    /**
     * Return a human-readable representation.
     * @return a human-readable representation.
     */
    public String toString() {
        return "a ThreadContext with transaction " + transaction + ", default timeout " + timeout + "s";
    }
}
