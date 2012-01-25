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

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.ThreadContext;
import bitronix.tm.utils.Scheduler;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.Map;

/**
 * Implementation of JTA 1.1 {@link TransactionSynchronizationRegistry}.
 *
 * @author lorban
 */
public class BitronixTransactionSynchronizationRegistry implements TransactionSynchronizationRegistry, Referenceable {

    private final BitronixTransactionManager transactionManager;

    public BitronixTransactionSynchronizationRegistry() {
        transactionManager = TransactionManagerServices.getTransactionManager();
    }

    public Object getResource(Object key) {
        try {
            if (key == null)
                throw new NullPointerException("key cannot be null");
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on current thread");

            return getResources().get(key);
        } catch (SystemException ex) {
            throw new BitronixRuntimeException("cannot get current transaction status", ex);
        }
    }

    public boolean getRollbackOnly() {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on current thread");

            return currentTransaction().getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new BitronixRuntimeException("cannot get current transaction status");
        }
    }

    public Object getTransactionKey() {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                return null;

            return currentTransaction().getGtrid();
        } catch (SystemException ex) {
            throw new BitronixRuntimeException("cannot get current transaction status", ex);
        }
    }

    public int getTransactionStatus() {
        try {
            if (currentTransaction() == null)
                return Status.STATUS_NO_TRANSACTION;

            return currentTransaction().getStatus();
        } catch (SystemException ex) {
            throw new BitronixRuntimeException("cannot get current transaction status", ex);
        }
    }

    public void putResource(Object key, Object value) {
        try {
            if (key == null)
                throw new NullPointerException("key cannot be null");
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on current thread");

            getResources().put(key, value);
        } catch (SystemException ex) {
            throw new BitronixRuntimeException("cannot get current transaction status", ex);
        }
    }

    public void registerInterposedSynchronization(Synchronization synchronization) {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on current thread");
            if (    currentTransaction().getStatus() == Status.STATUS_PREPARING ||
                    currentTransaction().getStatus() == Status.STATUS_PREPARED ||
                    currentTransaction().getStatus() == Status.STATUS_COMMITTING ||
                    currentTransaction().getStatus() == Status.STATUS_COMMITTED ||
                    currentTransaction().getStatus() == Status.STATUS_ROLLING_BACK ||
                    currentTransaction().getStatus() == Status.STATUS_ROLLEDBACK
                    )
                throw new IllegalStateException("transaction is done, cannot register an interposed synchronization");

            currentTransaction().getSynchronizationScheduler().add(synchronization, Scheduler.DEFAULT_POSITION -1);
        } catch (SystemException ex) {
            throw new BitronixRuntimeException("cannot get current transaction status", ex);
        }
    }

    public void setRollbackOnly() {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on current thread");

            currentTransaction().setStatus(Status.STATUS_MARKED_ROLLBACK);
        } catch (SystemException ex) {
            throw new BitronixRuntimeException("cannot get or set current transaction status", ex);
        }
    }

    private Map<Object, Object> getResources() {
        ThreadContext currentContext = transactionManager.currentThreadContext();
        if (currentContext == null) {
            return null;
        }
        return currentContext.getResources();
    }

    private BitronixTransaction currentTransaction() {
        return transactionManager.getCurrentTransaction();
    }

    public Reference getReference() throws NamingException {
        return new Reference(
                BitronixTransactionManager.class.getName(),
                new StringRefAddr("TransactionSynchronizationRegistry", "BitronixTransactionSynchronizationRegistry"),
                BitronixTransactionSynchronizationRegistryObjectFactory.class.getName(),
                null
        );
    }

}
