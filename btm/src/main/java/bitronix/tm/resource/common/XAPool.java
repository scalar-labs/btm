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
package bitronix.tm.resource.common;

import java.util.*;

import javax.transaction.Synchronization;
import javax.transaction.xa.XAResource;

import org.slf4j.*;

import bitronix.tm.*;
import bitronix.tm.internal.*;
import bitronix.tm.recovery.*;
import bitronix.tm.utils.*;
import bitronix.tm.utils.CryptoEngine;

/**
 * Generic XA pool. {@link XAStatefulHolder} instances are created by the {@link XAPool} out of a
 * {@link XAResourceProducer}. Those objects are then pooled and can be retrieved and/or recycled by the pool
 * depending on the running XA transaction's and the {@link XAStatefulHolder}'s states.
 *
 * @author lorban
 */
public class XAPool implements StateChangeListener {

    private final static Logger log = LoggerFactory.getLogger(XAPool.class);
    private final static String PASSWORD_PROPERTY_NAME = "password";

    private final Map<Uid, ThreadLocal<XAStatefulHolder>> statefulHolderTransactionMap = new HashMap<Uid, ThreadLocal<XAStatefulHolder>>();
    private final List<XAStatefulHolder> objects = new ArrayList<XAStatefulHolder>();
    private final ResourceBean bean;
    private final XAResourceProducer xaResourceProducer;
    private final Object xaFactory;
    private boolean failed = false;

    public XAPool(XAResourceProducer xaResourceProducer, ResourceBean bean) throws Exception {
        this.xaResourceProducer = xaResourceProducer;
        this.bean = bean;
        if (bean.getMaxPoolSize() < 1 || bean.getMinPoolSize() > bean.getMaxPoolSize())
            throw new IllegalArgumentException("cannot create a pool with min " + bean.getMinPoolSize() + " connection(s) and max " + bean.getMaxPoolSize() + " connection(s)");
        if (bean.getAcquireIncrement() < 1)
            throw new IllegalArgumentException("cannot create a pool with a connection acquisition increment less than 1, configured value is " + bean.getAcquireIncrement());

        xaFactory = createXAFactory(bean);
        init();

        if (bean.getIgnoreRecoveryFailures())
            log.warn("resource '" + bean.getUniqueName() + "' is configured to ignore recovery failures, make sure this setting is not enabled on a production system!");
    }

    private synchronized void init() throws Exception {
        growUntilMinPoolSize();

        if (bean.getMaxIdleTime() > 0) {
            TransactionManagerServices.getTaskScheduler().schedulePoolShrinking(this);
        }
    }

    public Object getXAFactory() {
        return xaFactory;
    }

    public synchronized void setFailed(boolean failed) {
        this.failed = failed;
    }

    public synchronized boolean isFailed() {
        return failed;
    }

    public synchronized Object getConnectionHandle() throws Exception {
        return getConnectionHandle(true);
    }
    
    public synchronized Object getConnectionHandle(boolean recycle) throws Exception {
        if (failed) {
            try {
                if (log.isDebugEnabled()) log.debug("resource '" + bean.getUniqueName() + "' is marked as failed, resetting and recovering it before trying connection acquisition");
                close();
                init();
                IncrementalRecoverer.recover(xaResourceProducer);
            }
            catch (RecoveryException ex) {
                throw new BitronixRuntimeException("incremental recovery failed when trying to acquire a connection from failed resource '" + bean.getUniqueName() + "'", ex);
            }
            catch (Exception ex) {
                throw new BitronixRuntimeException("pool reset failed when trying to acquire a connection from failed resource '" + bean.getUniqueName() + "'", ex);
            }
        }

        long remainingTime = bean.getAcquisitionTimeout() * 1000L;
        while (true) {
            long before = MonotonicClock.currentTimeMillis();
            XAStatefulHolder xaStatefulHolder = null;
            if (recycle) {
                if (bean.getShareTransactionConnections()) {
                    xaStatefulHolder = getSharedXAStatefulHolder();
                }
                else {
                    xaStatefulHolder = getNotAccessible();
                }
            }
            if (xaStatefulHolder == null) {
                xaStatefulHolder = getInPool();
            }
            if (log.isDebugEnabled()) log.debug("found " + Decoder.decodeXAStatefulHolderState(xaStatefulHolder.getState()) + " connection " + xaStatefulHolder + " from " + this);

            try {
                // getConnection() here could throw an exception, if it doesn't the connection is
                // still alive and we can share it (if sharing is enabled)
                Object connectionHandle = xaStatefulHolder.getConnectionHandle();
                if (bean.getShareTransactionConnections()) {
                    putSharedXAStatefulHolder(xaStatefulHolder);
                }
                growUntilMinPoolSize();
                return connectionHandle;
            } catch (Exception ex) {
                if (log.isDebugEnabled()) log.debug("connection is invalid, trying to close it", ex);
                try {
                    xaStatefulHolder.close();
                } catch (Exception ex2) {
                    if (log.isDebugEnabled()) log.debug("exception while trying to close invalid connection, ignoring it", ex2);
                }
                objects.remove(xaStatefulHolder);
                if (log.isDebugEnabled()) log.debug("removed invalid connection " + xaStatefulHolder + " from " + this);

                if (log.isDebugEnabled()) log.debug("waiting " + bean.getAcquisitionInterval() + "s before trying to acquire a connection again from " + this);
                long waitTime = bean.getAcquisitionInterval() * 1000L;
                if (waitTime > 0) {
                    try {
                        wait(waitTime);
                    } catch (InterruptedException ex2) {
                        // ignore
                    }
                }

                // check for timeout
                long now = MonotonicClock.currentTimeMillis();
                remainingTime -= (now - before);
                if (remainingTime <= 0) {
                    throw new BitronixRuntimeException("cannot get valid connection from " + this + " after trying for " + bean.getAcquisitionTimeout() + "s", ex);
                }
            }
        } // while true
    }

    public synchronized void close() {
        if (log.isDebugEnabled()) log.debug("closing all connections of " + this);
        for (XAStatefulHolder xaStatefulHolder : objects) {
            try {
                // This change is unrelated to BTM-35, but suppresses noise in the unit test output.
                // Connections that are already in STATE_CLOSED should not be closed again.
                if (xaStatefulHolder.getState() != XAStatefulHolder.STATE_CLOSED) {
                    xaStatefulHolder.close();
                }
            } catch (Exception ex) {
                if (log.isDebugEnabled()) log.debug("ignoring exception while closing connection " + xaStatefulHolder, ex);
            }
        }

        if (TransactionManagerServices.isTaskSchedulerRunning())
            TransactionManagerServices.getTaskScheduler().cancelPoolShrinking(this);

        objects.clear();
        failed = false;
    }

    public synchronized long totalPoolSize() {
        return objects.size();
    }

    public synchronized long inPoolSize() {
        int count = 0;
        for (XAStatefulHolder xaStatefulHolder : objects) {
            if (xaStatefulHolder.getState() == XAStatefulHolder.STATE_IN_POOL)
                count++;
        }
        return count;
    }

    public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
        if (newState == XAStatefulHolder.STATE_IN_POOL) {
            if (log.isDebugEnabled()) log.debug("a connection's state changed to IN_POOL, notifying a thread eventually waiting for a connection");
            synchronized (this) {
                notify();
            }
        }
    }

    public void stateChanging(XAStatefulHolder source, int currentState, int futureState) {
    }

    public synchronized XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        for (XAStatefulHolder xaStatefulHolder : objects) {
            List<XAResourceHolder> xaResourceHolders = xaStatefulHolder.getXAResourceHolders();

            for (XAResourceHolder holder : xaResourceHolders) {
                if (holder.getXAResource() == xaResource)
                    return holder;
            }
        }
        return null;
    }

    // used for testing
    List<XAStatefulHolder> getXAResourceHolders() {
        return Collections.unmodifiableList(objects);
    }

    public Date getNextShrinkDate() {
        return new Date(MonotonicClock.currentTimeMillis() + bean.getMaxIdleTime() * 1000);
    }

    public synchronized void shrink() throws Exception {
        if (log.isDebugEnabled()) log.debug("shrinking " + this);
        List<XAStatefulHolder> toRemoveXaStatefulHolders = new ArrayList<XAStatefulHolder>();
        long now = MonotonicClock.currentTimeMillis();
        for (XAStatefulHolder xaStatefulHolder : objects) {
            if (xaStatefulHolder.getState() != XAStatefulHolder.STATE_IN_POOL)
                continue;

            long expirationTime = (xaStatefulHolder.getLastReleaseDate().getTime() + (bean.getMaxIdleTime() * 1000));
            if (log.isDebugEnabled()) log.debug("checking if connection can be closed: " + xaStatefulHolder + " - closing time: " + expirationTime + ", now time: " + now);
            if (expirationTime <= now) {
                try {
                    xaStatefulHolder.close();
                } catch (Exception ex) {
                    log.warn("error closing " + xaStatefulHolder, ex);
                }
                toRemoveXaStatefulHolders.add(xaStatefulHolder);
            }
        } // for
        if (log.isDebugEnabled()) log.debug("closed " + toRemoveXaStatefulHolders.size() + " idle connection(s)");
        objects.removeAll(toRemoveXaStatefulHolders);
        growUntilMinPoolSize();
        if (log.isDebugEnabled()) log.debug("shrunk " + this);
    }

    public synchronized void reset() throws Exception {
        if (log.isDebugEnabled()) log.debug("resetting " + this);
        List<XAStatefulHolder> toRemoveXaStatefulHolders = new ArrayList<XAStatefulHolder>();
        for (XAStatefulHolder xaStatefulHolder : objects) {
            if (xaStatefulHolder.getState() != XAStatefulHolder.STATE_IN_POOL)
                continue;

            try {
                xaStatefulHolder.close();
            } catch (Exception ex) {
                log.warn("error closing " + xaStatefulHolder, ex);
            }
            toRemoveXaStatefulHolders.add(xaStatefulHolder);
        }
        if (log.isDebugEnabled()) log.debug("closed " + toRemoveXaStatefulHolders.size() + " connection(s)");
        objects.removeAll(toRemoveXaStatefulHolders);
        growUntilMinPoolSize();
        if (log.isDebugEnabled()) log.debug("reset " + this);
    }

    public String toString() {
        return "an XAPool of resource " + bean.getUniqueName() + " with " + totalPoolSize() + " connection(s) (" + inPoolSize() + " still available)" + (failed ? " -failed-" : "");
    }

    private synchronized void createPooledObject(Object xaFactory) throws Exception {
        XAStatefulHolder xaStatefulHolder = xaResourceProducer.createPooledConnection(xaFactory, bean);
        xaStatefulHolder.addStateChangeEventListener(this);
        objects.add(xaStatefulHolder);
    }

    private static Object createXAFactory(ResourceBean bean) throws Exception {
        String className = bean.getClassName();
        if (className == null)
            throw new IllegalArgumentException("className cannot be null");
        Class xaFactoryClass = ClassLoaderUtils.loadClass(className);
        Object xaFactory = xaFactoryClass.newInstance();

        for (Map.Entry<Object, Object> entry : bean.getDriverProperties().entrySet()) {
            String name = (String) entry.getKey();
            Object value = entry.getValue();

            if (name.endsWith(PASSWORD_PROPERTY_NAME)) {
                value = decrypt((String) value);
            }

            if (log.isDebugEnabled()) log.debug("setting vendor property '" + name + "' to '" + value + "'");
            PropertyUtils.setProperty(xaFactory, name, value);
        }
        return xaFactory;
    }

    private static String decrypt(String resourcePassword) throws Exception {
        int startIdx = resourcePassword.indexOf("{");
        int endIdx = resourcePassword.indexOf("}");

        if (startIdx != 0 || endIdx == -1)
            return resourcePassword;

        String cipher = resourcePassword.substring(1, endIdx);
        if (log.isDebugEnabled()) log.debug("resource password is encrypted, decrypting " + resourcePassword);
        return CryptoEngine.decrypt(cipher, resourcePassword.substring(endIdx + 1));
    }

    private synchronized XAStatefulHolder getNotAccessible() {
        if (log.isDebugEnabled()) log.debug("trying to recycle a NOT_ACCESSIBLE connection of " + this);
        BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
        if (transaction == null) {
            if (log.isDebugEnabled()) log.debug("no current transaction, no connection can be in state NOT_ACCESSIBLE when there is no global transaction context");
            return null;
        }
        Uid currentTxGtrid = transaction.getResourceManager().getGtrid();
        if (log.isDebugEnabled()) log.debug("current transaction GTRID is [" + currentTxGtrid + "]");

        for (XAStatefulHolder xaStatefulHolder : objects) {
            if (xaStatefulHolder.getState() == XAStatefulHolder.STATE_NOT_ACCESSIBLE) {
                if (log.isDebugEnabled()) log.debug("found a connection in NOT_ACCESSIBLE state: " + xaStatefulHolder);
                if (containsXAResourceHolderMatchingGtrid(xaStatefulHolder, currentTxGtrid))
                    return xaStatefulHolder;

            }
        } // for

        if (log.isDebugEnabled()) log.debug("no NOT_ACCESSIBLE connection enlisted in this transaction");
        return null;
    }

    private static boolean containsXAResourceHolderMatchingGtrid(XAStatefulHolder xaStatefulHolder, Uid currentTxGtrid) {
        List<XAResourceHolder> xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
        if (log.isDebugEnabled()) log.debug(xaResourceHolders.size() + " xa resource(s) created by connection in NOT_ACCESSIBLE state: " + xaStatefulHolder);
        for (XAResourceHolder xaResourceHolder : xaResourceHolders) {
            Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolder.getXAResourceHolderStatesForGtrid(currentTxGtrid);
            if (statesForGtrid == null)
                return false;

            for (XAResourceHolderState xaResourceHolderState : statesForGtrid.values()) {
                // compare GTRIDs
                BitronixXid bitronixXid = xaResourceHolderState.getXid();
                Uid resourceGtrid = bitronixXid.getGlobalTransactionIdUid();
                if (log.isDebugEnabled()) log.debug("NOT_ACCESSIBLE xa resource GTRID: " + resourceGtrid);
                if (currentTxGtrid.equals(resourceGtrid)) {
                    if (log.isDebugEnabled()) log.debug("NOT_ACCESSIBLE xa resource's GTRID matched this transaction's GTRID, recycling it");
                    return true;
                }
            }
        }
        return false;
    }

    private synchronized XAStatefulHolder getInPool() throws Exception {
        if (log.isDebugEnabled()) log.debug("getting a IN_POOL connection from " + this);

        if (inPoolSize() == 0) {
            if (log.isDebugEnabled()) log.debug("no more free connection in " + this + ", trying to grow it");
            grow();
        }

        waitForConnectionInPool();
        for (XAStatefulHolder xaStatefulHolder : objects) {
            if (xaStatefulHolder.getState() == XAStatefulHolder.STATE_IN_POOL)
                return xaStatefulHolder;
        }
        throw new BitronixRuntimeException("pool does not contain IN_POOL connection while it should !");
    }

    private synchronized void grow() throws Exception {
        if (totalPoolSize() < bean.getMaxPoolSize()) {
            long increment = bean.getAcquireIncrement();
            if (totalPoolSize() + increment > bean.getMaxPoolSize()) {
                increment = bean.getMaxPoolSize() - totalPoolSize();
            }

            if (log.isDebugEnabled()) log.debug("incrementing " + bean.getUniqueName() + " pool size by " + increment + " unit(s) to reach " + (totalPoolSize() + increment) + " connection(s)");
            for (int i=0; i < increment ;i++) {
                createPooledObject(xaFactory);
            }
        } else {
            if (log.isDebugEnabled()) log.debug("pool " + bean.getUniqueName() + " already at max size of " + totalPoolSize() + " connection(s), not growing it");
        }
    }

    private synchronized void growUntilMinPoolSize() throws Exception {
        for (int i = (int)totalPoolSize(); i < bean.getMinPoolSize() ;i++) {
            createPooledObject(xaFactory);
        }
    }

    private synchronized void waitForConnectionInPool() {
        long remainingTime = bean.getAcquisitionTimeout() * 1000L;
        if (log.isDebugEnabled()) log.debug("waiting for IN_POOL connections count to be > 0, currently is " + inPoolSize());
        while (inPoolSize() == 0) {
            long before = MonotonicClock.currentTimeMillis();
            try {
                if (log.isDebugEnabled()) log.debug("waiting " + remainingTime + "ms");
                wait(remainingTime);
                if (log.isDebugEnabled()) log.debug("waiting over, IN_POOL connections count is now " + inPoolSize());
            } catch (InterruptedException ex) {
                // ignore
            }

            long now = MonotonicClock.currentTimeMillis();
            remainingTime -= (now - before);
            if (remainingTime <= 0 && inPoolSize() == 0) {
                if (log.isDebugEnabled()) log.debug("connection pool dequeue timed out");
                if (TransactionManagerServices.isTransactionManagerRunning())
                    TransactionManagerServices.getTransactionManager().dumpTransactionContexts();
                throw new BitronixRuntimeException("XA pool of resource " + bean.getUniqueName() + " still empty after " + bean.getAcquisitionTimeout() + "s wait time");
            }
        } // while
    }

    /**
     * Shared Connection Handling
     */

    /**
     * Try to get a shared XAStatefulHolder.  This method will either return
     * a shared XAStatefulHolder or <code>null</code>.  If there is no current
     * transaction, XAStatefulHolder's are not shared.  If there is a transaction
     * <i>and</i> there is a XAStatefulHolder associated with this thread already,
     * we return that XAStatefulHolder (provided it is ACCESSIBLE or NOT_ACCESSIBLE).
     *
     * @return a shared XAStatefulHolder or <code>null</code>
     */
    private synchronized XAStatefulHolder getSharedXAStatefulHolder() {
        BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
        if (transaction == null) {
            if (log.isDebugEnabled()) log.debug("no current transaction, shared connection map will not be used");
            return null;
        }
        Uid currentTxGtrid = transaction.getResourceManager().getGtrid();

        ThreadLocal<XAStatefulHolder> threadLocal = statefulHolderTransactionMap.get(currentTxGtrid);
        if (threadLocal != null) {
            XAStatefulHolder xaStatefulHolder = threadLocal.get();
            // Additional sanity checks...
            if (xaStatefulHolder != null &&
                xaStatefulHolder.getState() != XAStatefulHolder.STATE_IN_POOL &&
                xaStatefulHolder.getState() != XAStatefulHolder.STATE_CLOSED) {

                if (log.isDebugEnabled()) log.debug("sharing connection " + xaStatefulHolder + " in transaction " + currentTxGtrid);
                return xaStatefulHolder;
            }
        }

        return null;
    }

    /**
     * Try to share a XAStatefulHolder with other callers on this thread.  If
     * there is no current transaction, the XAStatefulHolder is not put into the
     * ThreadLocal.  If there is a transaction, and it is the first time we're
     * attempting to share a XAStatefulHolder on this thread, then we register
     * a Synchronization so we can pull the ThreadLocals out of the shared map
     * when the transaction completes (either commit() or rollback()).  Without
     * the Synchronization we would "leak".
     *
     * @param xaStatefulHolder a XAStatefulHolder to share with other callers
     *    on this thread.
     */
    private synchronized void putSharedXAStatefulHolder(XAStatefulHolder xaStatefulHolder) {
        BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
        if (transaction == null) {
            if (log.isDebugEnabled()) log.debug("no current transaction, not adding " + xaStatefulHolder + " to shared connection map");
            return;
        }
        final Uid currentTxGtrid = transaction.getResourceManager().getGtrid();

        ThreadLocal<XAStatefulHolder> threadLocal = statefulHolderTransactionMap.get(currentTxGtrid);
        if (threadLocal == null) {
            // This is the first time this TxGtrid/ThreadLocal is going into the map,
            // register interest in synchronization so we can remove it at commit/rollback
            try {
                transaction.registerSynchronization(new SharedStatefulHolderCleanupSynchronization(currentTxGtrid));
            } catch (Exception e) {
                // OK, forget it.  The transaction is either rollback only or already finished.
                return;
            }

            threadLocal = new ThreadLocal<XAStatefulHolder>();
            statefulHolderTransactionMap.put(currentTxGtrid, threadLocal);
            if (log.isDebugEnabled()) log.debug("added shared connection mapping for " + currentTxGtrid + " holder " + xaStatefulHolder);           
        }

        // Set the XAStatefulHolder on the ThreadLocal.  Even if we've already set it before,
        // it's safe -- checking would be more expensive than just setting it again.
        threadLocal.set(xaStatefulHolder);
    }

    private final class SharedStatefulHolderCleanupSynchronization implements Synchronization {
        private final Uid gtrid;

        private SharedStatefulHolderCleanupSynchronization(Uid gtrid) {
            this.gtrid = gtrid;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            synchronized (XAPool.this) {
                statefulHolderTransactionMap.remove(gtrid);
                if (log.isDebugEnabled()) log.debug("deleted shared connection mappings for " + gtrid);
            }
        }

        public String toString() {
            return "a SharedStatefulHolderCleanupSynchronization with GTRID [" + gtrid + "]";
        }
    }
}
