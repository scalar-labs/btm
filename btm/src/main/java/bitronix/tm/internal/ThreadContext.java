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
package bitronix.tm.internal;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional context of a thread. It contains both the active transaction (if any) and all default parameters
 * that a transaction running on a thread must inherit.
 *
 * @author lorban
 */
public class ThreadContext {

    private final static Logger log = LoggerFactory.getLogger(ThreadContext.class);

    private volatile BitronixTransaction transaction;
    private volatile int timeout = TransactionManagerServices.getConfiguration().getDefaultTransactionTimeout();;

    private static ThreadLocal<ThreadContext> threadContext = new ThreadLocal<ThreadContext>() {
        protected ThreadContext initialValue() {
            return new ThreadContext();
        }
    };

    /**
     * Private constructor.
     */
    private ThreadContext() {
        // Can only be constructed from initialValue() above.
    }

    /**
     * Get the ThreadContext thread local value for the calling thread. This is
     * the only way to access the ThreadContext. The get() call will
     * automatically construct a ThreadContext if this thread doesn't have one
     * (see initialValue() above).
     * 
     * @return the calling thread's ThreadContext
     */
    public static ThreadContext getThreadContext() {
        return threadContext.get();
    }

    /**
     * Return the transaction linked with this ThreadContext.
     * 
     * @return the transaction linked to this ThreadContext or null if there is none.
     */
    public BitronixTransaction getTransaction() {
        return transaction;
    }

    /**
     * Link a transaction with this ThreadContext.
     * 
     * @param transaction the transaction to link.
     */
    public void setTransaction(BitronixTransaction transaction) {
        if (transaction == null)
            throw new IllegalArgumentException("transaction parameter cannot be null");
        if (log.isDebugEnabled()) { log.debug("assigning <" + transaction + "> to <" + this + ">"); }
        this.transaction = transaction;
    }

    /**
     * Clean the transaction from this ThreadContext
     */
    public void clearTransaction() {
        transaction = null;
    }

    /**
     * Return this context's default timeout.
     * 
     * @return this context's default timeout.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Set this context's default timeout. All transactions started by the
     * thread linked to this context will get this value as their default
     * timeout.
     * 
     * @param timeout the new default timeout value in seconds.
     */
    public void setTimeout(int timeout) {
        if (timeout == 0) {
            int defaultValue = TransactionManagerServices.getConfiguration().getDefaultTransactionTimeout();
            if (log.isDebugEnabled()) { log.debug("resetting default timeout of thread context to default value of " + defaultValue + "s"); }
            this.timeout = defaultValue;
        }
        else {    
            if (log.isDebugEnabled()) { log.debug("changing default timeout of thread context to " + timeout + "s"); }
            this.timeout = timeout;
        }
    }

    /**
     * Return a human-readable representation.
     * 
     * @return a human-readable representation.
     */
    public String toString() {
        return "a ThreadContext (" + System.identityHashCode(this) + ") with transaction " + transaction + ", default timeout " + timeout + "s";
    }
}
