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
package bitronix.tm.twopc.executor;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.XAResourceHolderState;

import javax.transaction.xa.XAException;

/**
 * Abstract job definition executable by the 2PC thread pools.
 *
 * @author lorban
 */
public abstract class Job implements Runnable {
    private final XAResourceHolderState resourceHolder;

    private volatile Object future;
    protected volatile XAException xaException;
    protected volatile RuntimeException runtimeException;

    public Job(XAResourceHolderState resourceHolder) {
        this.resourceHolder = resourceHolder;
    }

    public XAResourceHolderState getResource() {
        return resourceHolder;
    }

    public XAException getXAException() {
        return xaException;
    }

    public RuntimeException getRuntimeException() {
        return runtimeException;
    }

    public void setFuture(Object future) {
        this.future = future;
    }

    public Object getFuture() {
        return future;
    }

    public final void run() {
        String oldThreadName = null;
        if (TransactionManagerServices.getConfiguration().isAsynchronous2Pc()) {
            oldThreadName = Thread.currentThread().getName();
            Thread.currentThread().setName("bitronix-2pc [ " +
                    resourceHolder.getXid().toString() +
                    " ]");
        }
        execute();
        if (oldThreadName != null) {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    protected abstract void execute();
}
