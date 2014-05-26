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

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.utils.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of JTA 1.1 {@link TransactionSynchronizationRegistry}.
 *
 * @author Ludovic Orban
 */
public class BitronixTransactionSynchronizationRegistry implements TransactionSynchronizationRegistry, Referenceable {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransactionSynchronizationRegistry.class);

    private final BitronixTransactionManager transactionManager;

    private final static ThreadLocal<Map<Object, Object>> resourcesTl = new ThreadLocal<Map<Object, Object>>() {
        @Override
        protected Map<Object, Object> initialValue() {
            return new HashMap<Object, Object>();
        }
    };


    public BitronixTransactionSynchronizationRegistry() {
        transactionManager = TransactionManagerServices.getTransactionManager();
    }

    @Override
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

    @Override
    public boolean getRollbackOnly() {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on current thread");

            return currentTransaction().getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new BitronixRuntimeException("cannot get current transaction status");
        }
    }

    @Override
    public Object getTransactionKey() {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                return null;

            return currentTransaction().getGtrid();
        } catch (SystemException ex) {
            throw new BitronixRuntimeException("cannot get current transaction status", ex);
        }
    }

    @Override
    public int getTransactionStatus() {
        try {
            if (currentTransaction() == null)
                return Status.STATUS_NO_TRANSACTION;

            return currentTransaction().getStatus();
        } catch (SystemException ex) {
            throw new BitronixRuntimeException("cannot get current transaction status", ex);
        }
    }

    @Override
    public void putResource(Object key, Object value) {
        try {
            if (key == null)
                throw new NullPointerException("key cannot be null");
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on current thread");

            Object oldValue = getResources().put(key, value);

            if (oldValue == null && getResources().size() == 1) {
                if (log.isDebugEnabled()) { log.debug("first resource put in synchronization registry, registering a ClearRegistryResourcesSynchronization"); }
                Synchronization synchronization = new ClearRegistryResourcesSynchronization();
                currentTransaction().getSynchronizationScheduler().add(synchronization, Scheduler.ALWAYS_LAST_POSITION);
            }
        } catch (SystemException ex) {
            throw new BitronixRuntimeException("cannot get current transaction status", ex);
        }
    }

    @Override
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

    @Override
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
        return resourcesTl.get();
    }

    private BitronixTransaction currentTransaction() {
        return transactionManager.getCurrentTransaction();
    }

    @Override
    public Reference getReference() throws NamingException {
        return new Reference(
                BitronixTransactionManager.class.getName(),
                new StringRefAddr("TransactionSynchronizationRegistry", "BitronixTransactionSynchronizationRegistry"),
                BitronixTransactionSynchronizationRegistryObjectFactory.class.getName(),
                null
        );
    }

    private final class ClearRegistryResourcesSynchronization implements Synchronization {
        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int status) {
            if (log.isDebugEnabled()) { log.debug("clearing resources"); }
            getResources().clear();
        }
    }

}
