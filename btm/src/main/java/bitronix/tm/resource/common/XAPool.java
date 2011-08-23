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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.transaction.Synchronization;
import javax.transaction.xa.XAResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.IncrementalRecoverer;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.CryptoEngine;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.MonotonicClock;
import bitronix.tm.utils.PropertyUtils;
import bitronix.tm.utils.Uid;

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

    private final Map<Uid, StatefulHolderThreadLocal> statefulHolderTransactionMap = new HashMap<Uid, StatefulHolderThreadLocal>();
    private final BlockingQueue<XAStatefulHolder> availablePool = new LinkedBlockingQueue<XAStatefulHolder>();
    private final Queue<XAStatefulHolder> accessiblePool = new ConcurrentLinkedQueue<XAStatefulHolder>();
    private final Queue<XAStatefulHolder> inaccessiblePool = new ConcurrentLinkedQueue<XAStatefulHolder>();
    private final Queue<WeakReference<XAStatefulHolder>> closedPool = new ConcurrentLinkedQueue<WeakReference<XAStatefulHolder>>();
    private final ResourceBean bean;
    private final XAResourceProducer xaResourceProducer;
    private final Object xaFactory;
    private volatile boolean failed = false;

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

    private void init() throws Exception {
        growUntilMinPoolSize();

        if (bean.getMaxIdleTime() > 0) {
            TransactionManagerServices.getTaskScheduler().schedulePoolShrinking(this);
        }
    }

    private void growUntilMinPoolSize() throws Exception {
        for (int i = (int)totalPoolSize(); i < bean.getMinPoolSize() ;i++) {
            createPooledObject(xaFactory);
        }
    }

    public Object getXAFactory() {
        return xaFactory;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public boolean isFailed() {
        return failed;
    }

    public Object getConnectionHandle() throws Exception {
        return getConnectionHandle(true);
    }
    
    public synchronized Object getConnectionHandle(boolean recycle) throws Exception {
        if (failed) {
            try {
                if (log.isDebugEnabled()) { log.debug("resource '" + bean.getUniqueName() + "' is marked as failed, resetting and recovering it before trying connection acquisition"); }
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

        long before = MonotonicClock.currentTimeMillis();
        while (true) {
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
            if (log.isDebugEnabled()) { log.debug("found " + Decoder.decodeXAStatefulHolderState(xaStatefulHolder.getState()) + " connection " + xaStatefulHolder + " from " + this); }

            try {
                // getConnection() here could throw an exception, if it doesn't the connection is
                // still alive and we can share it (if sharing is enabled)
                Object connectionHandle = xaStatefulHolder.getConnectionHandle();
                if (bean.getShareTransactionConnections()) {
                    putSharedXAStatefulHolder(xaStatefulHolder);
                }                
                return connectionHandle;
            } catch (Exception ex) {
                if (log.isDebugEnabled()) { log.debug("connection is invalid, trying to close it", ex); }
                try {
                    xaStatefulHolder.close();
                } catch (Exception ex2) {
                    if (log.isDebugEnabled()) { log.debug("exception while trying to close invalid connection, ignoring it", ex2); }
                }
                finally {
                    if (xaStatefulHolder.getState() != XAStatefulHolder.STATE_CLOSED) {
                        stateChanged(xaStatefulHolder, xaStatefulHolder.getState(), XAStatefulHolder.STATE_CLOSED);
                    }
                    if (log.isDebugEnabled()) { log.debug("removed invalid connection " + xaStatefulHolder + " from " + this); }
                }

                if (log.isDebugEnabled()) { log.debug("waiting " + bean.getAcquisitionInterval() + "s before trying to acquire a connection again from " + this); }
                try {
                    wait(bean.getAcquisitionInterval() * 1000L);
                } catch (InterruptedException ex2) {
                    // ignore
                }

                // check for timeout
                long now = MonotonicClock.currentTimeMillis();
                long remainingTime = bean.getAcquisitionTimeout() * 1000L;
                remainingTime -= (now - before);
                if (remainingTime <= 0) {
                    throw new BitronixRuntimeException("cannot get valid connection from " + this + " after trying for " + bean.getAcquisitionTimeout() + "s", ex);
                }
            }
        } // while true
    }

    public synchronized void close() {
        if (log.isDebugEnabled()) { log.debug("closing all connections of " + this); }
        
        for (XAStatefulHolder xaStatefulHolder : getXAResourceHolders()) {
            try {
                xaStatefulHolder.close();
            } catch (Exception ex) {
                if (log.isDebugEnabled()) { log.debug("ignoring exception while closing connection " + xaStatefulHolder, ex); }
            }
        }

        if (TransactionManagerServices.isTaskSchedulerRunning())
            TransactionManagerServices.getTaskScheduler().cancelPoolShrinking(this);

        availablePool.clear();
        accessiblePool.clear();
        inaccessiblePool.clear();
        closedPool.clear();
        failed = false;
    }

    public synchronized long totalPoolSize() {
        return availablePool.size() + accessiblePool.size() + inaccessiblePool.size();
    }

    public long inPoolSize() {
    	return availablePool.size();
    }

    public synchronized XAResourceHolder findXAResourceHolder(XAResource xaResource) {
    	for (XAStatefulHolder xaStatefulHolder : getXAResourceHolders()) {
            for (XAResourceHolder holder : xaStatefulHolder.getXAResourceHolders()) {
                if (holder.getXAResource() == xaResource)
                    return holder;
            }
        }
        return null;
    }

    public List<XAStatefulHolder> getXAResourceHolders() {
    	List<XAStatefulHolder> holders = new ArrayList<XAStatefulHolder>();
    	holders.addAll(availablePool);
    	holders.addAll(accessiblePool);
    	holders.addAll(inaccessiblePool);
        return holders;
    }

    public Date getNextShrinkDate() {
        return new Date(MonotonicClock.currentTimeMillis() + bean.getMaxIdleTime() * 1000);
    }

    public synchronized void shrink() throws Exception {
        if (log.isDebugEnabled()) { log.debug("shrinking " + this); }
        List<XAStatefulHolder> toRemoveXaStatefulHolders = new ArrayList<XAStatefulHolder>();
        long now = MonotonicClock.currentTimeMillis();
        for (XAStatefulHolder xaStatefulHolder : availablePool) {
            long expirationTime = (xaStatefulHolder.getLastReleaseDate().getTime() + (bean.getMaxIdleTime() * 1000));
            if (log.isDebugEnabled()) { log.debug("checking if connection can be closed: " + xaStatefulHolder + " - closing time: " + expirationTime + ", now time: " + now); }
            if (expirationTime <= now) {
                try {
                    xaStatefulHolder.close();
                } catch (Exception ex) {
                    log.warn("error closing " + xaStatefulHolder, ex);
                }
                toRemoveXaStatefulHolders.add(xaStatefulHolder);
            }
        } // for
        if (log.isDebugEnabled()) { log.debug("closed " + toRemoveXaStatefulHolders.size() + " idle connection(s)"); }
        availablePool.removeAll(toRemoveXaStatefulHolders);
        growUntilMinPoolSize();
        if (log.isDebugEnabled()) { log.debug("shrunk " + this); }
    }

    public synchronized void reset() throws Exception {
        if (log.isDebugEnabled()) { log.debug("resetting " + this); }
        List<XAStatefulHolder> toRemoveXaStatefulHolders = new ArrayList<XAStatefulHolder>();
        for (XAStatefulHolder xaStatefulHolder : availablePool) {
            try {
                xaStatefulHolder.close();
            } catch (Exception ex) {
                log.warn("error closing " + xaStatefulHolder, ex);
            }
            toRemoveXaStatefulHolders.add(xaStatefulHolder);
        }
        if (log.isDebugEnabled()) { log.debug("closed " + toRemoveXaStatefulHolders.size() + " connection(s)"); }
        availablePool.removeAll(toRemoveXaStatefulHolders);
        growUntilMinPoolSize();
        if (log.isDebugEnabled()) { log.debug("reset " + this); }
    }

    public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
    	// remove from the former pool...
    	removeFromPoolByState(source, oldState);

    	// ...and add to the new pool
    	switch (newState) {
    	case XAStatefulHolder.STATE_IN_POOL:
    		if (log.isDebugEnabled()) { log.debug("added " + source + " to the available pool"); }
    		availablePool.add(source);
    		break;
    	case XAStatefulHolder.STATE_ACCESSIBLE:
    		if (log.isDebugEnabled()) { log.debug("added " + source + " to the accessible pool"); }
    		accessiblePool.add(source);
    		break;
    	case XAStatefulHolder.STATE_NOT_ACCESSIBLE:
    		if (log.isDebugEnabled()) { log.debug("added " + source + " to the inaccessible pool"); }
    		inaccessiblePool.add(source);
    		break;
    	case XAStatefulHolder.STATE_CLOSED:
    		if (log.isDebugEnabled()) { log.debug("added " + source + " to the closed pool"); }
    		closedPool.add(new WeakReference<XAStatefulHolder>(source));
    		break;
    	}
    }

    private void removeFromPoolByState(XAStatefulHolder source, int state) {
    	switch (state) {
    	case XAStatefulHolder.STATE_IN_POOL:
    		if (log.isDebugEnabled()) { log.debug("removed " + source + " from the available pool"); }
    		availablePool.remove(source);
    		break;
    	case XAStatefulHolder.STATE_ACCESSIBLE:
    		if (log.isDebugEnabled()) { log.debug("removed " + source + " from the accessible pool"); }
    		accessiblePool.remove(source);
    		break;
    	case XAStatefulHolder.STATE_NOT_ACCESSIBLE:
    		if (log.isDebugEnabled()) { log.debug("removed " + source + " from the inaccessible pool"); }
    		inaccessiblePool.remove(source);
    		break;
    	case XAStatefulHolder.STATE_CLOSED:
    	    closedPool.remove(source);
    		break;
    	}
    }

    public void stateChanging(XAStatefulHolder source, int currentState, int futureState) {
    }


    public String toString() {
        return "an XAPool of resource " + bean.getUniqueName() + " with " + totalPoolSize() + " connection(s) (" + inPoolSize() + " still available)" + (failed ? " -failed-" : "");
    }

    private void createPooledObject(Object xaFactory) throws Exception {
        XAStatefulHolder xaStatefulHolder = xaResourceProducer.createPooledConnection(xaFactory, bean);
        xaStatefulHolder.addStateChangeEventListener(this);
        availablePool.add(xaStatefulHolder);
    }

    private static Object createXAFactory(ResourceBean bean) throws Exception {
        String className = bean.getClassName();
        if (className == null)
            throw new IllegalArgumentException("className cannot be null");
        Class<?> xaFactoryClass = ClassLoaderUtils.loadClass(className);
        Object xaFactory = xaFactoryClass.newInstance();

        for (Map.Entry<Object, Object> entry : bean.getDriverProperties().entrySet()) {
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (name.endsWith(PASSWORD_PROPERTY_NAME)) {
                value = decrypt(value);
            }

            if (log.isDebugEnabled()) { log.debug("setting vendor property '" + name + "' to '" + value + "'"); }
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
        if (log.isDebugEnabled()) { log.debug("resource password is encrypted, decrypting " + resourcePassword); }
        return CryptoEngine.decrypt(cipher, resourcePassword.substring(endIdx + 1));
    }

    private XAStatefulHolder getNotAccessible() {
        if (log.isDebugEnabled()) { log.debug("trying to recycle a NOT_ACCESSIBLE connection of " + this); }
        BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
        if (transaction == null) {
            if (log.isDebugEnabled()) { log.debug("no current transaction, no connection can be in state NOT_ACCESSIBLE when there is no global transaction context"); }
            return null;
        }
        Uid currentTxGtrid = transaction.getResourceManager().getGtrid();
        if (log.isDebugEnabled()) { log.debug("current transaction GTRID is [" + currentTxGtrid + "]"); }

        for (XAStatefulHolder xaStatefulHolder : inaccessiblePool) {
            if (log.isDebugEnabled()) { log.debug("found a connection in NOT_ACCESSIBLE state: " + xaStatefulHolder); }
            if (containsXAResourceHolderMatchingGtrid(xaStatefulHolder, currentTxGtrid))
                return xaStatefulHolder;
        } // for

        if (log.isDebugEnabled()) { log.debug("no NOT_ACCESSIBLE connection enlisted in this transaction"); }
        return null;
    }

    private boolean containsXAResourceHolderMatchingGtrid(XAStatefulHolder xaStatefulHolder, Uid currentTxGtrid) {
        List<XAResourceHolder> xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
        if (log.isDebugEnabled()) { log.debug(xaResourceHolders.size() + " xa resource(s) created by connection in NOT_ACCESSIBLE state: " + xaStatefulHolder); }
        for (XAResourceHolder xaResourceHolder : xaStatefulHolder.getXAResourceHolders()) {
            Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolder.getXAResourceHolderStatesForGtrid(currentTxGtrid);
            if (statesForGtrid == null)
                return false;

            for (XAResourceHolderState xaResourceHolderState : statesForGtrid.values()) {
                // compare GTRIDs
                BitronixXid bitronixXid = xaResourceHolderState.getXid();
                Uid resourceGtrid = bitronixXid.getGlobalTransactionIdUid();
                if (log.isDebugEnabled()) { log.debug("NOT_ACCESSIBLE xa resource GTRID: " + resourceGtrid); }
                if (currentTxGtrid.equals(resourceGtrid)) {
                    if (log.isDebugEnabled()) { log.debug("NOT_ACCESSIBLE xa resource's GTRID matched this transaction's GTRID, recycling it"); }
                    return true;
                }
            }
        }
        return false;
    }

    private synchronized XAStatefulHolder getInPool() throws Exception {
        if (log.isDebugEnabled()) { log.debug("getting a IN_POOL connection from " + this); }

        if (inPoolSize() == 0) {
            if (log.isDebugEnabled()) { log.debug("no more free connection in " + this + ", trying to grow it"); }
            grow();
        }

        if (log.isDebugEnabled()) { log.debug("getting IN_POOL connection, waiting if necessary, current size is " + inPoolSize()); }

        try {
        	XAStatefulHolder xaStatefulHolder = availablePool.poll(bean.getAcquisitionTimeout(), TimeUnit.SECONDS);
        	if (xaStatefulHolder != null) {
        		return xaStatefulHolder;
        	}
        	throw new BitronixRuntimeException("XA pool of resource " + bean.getUniqueName() + " still empty after " + bean.getAcquisitionTimeout() + "s wait time");
		} catch (InterruptedException e) {
			throw new BitronixRuntimeException("Interrupted while waiting for IN_POOL connection.");
		}
    }

    private synchronized void grow() throws Exception {
        if (totalPoolSize() < bean.getMaxPoolSize()) {
            long increment = bean.getAcquireIncrement();
            if (totalPoolSize() + increment > bean.getMaxPoolSize()) {
                increment = bean.getMaxPoolSize() - totalPoolSize();
            }

            if (log.isDebugEnabled()) { log.debug("incrementing " + bean.getUniqueName() + " pool size by " + increment + " unit(s) to reach " + (totalPoolSize() + increment) + " connection(s)"); }
            for (int i=0; i < increment ;i++) {
                createPooledObject(xaFactory);
            }
        }
        else {
            if (log.isDebugEnabled()) { log.debug("pool " + bean.getUniqueName() + " already at max size of " + totalPoolSize() + " connection(s), not growing it"); }
        }
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
    private XAStatefulHolder getSharedXAStatefulHolder() {
        BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
        if (transaction == null) {
            if (log.isDebugEnabled()) { log.debug("no current transaction, shared connection map will not be used"); }
            return null;
        }
        Uid currentTxGtrid = transaction.getResourceManager().getGtrid();

        StatefulHolderThreadLocal threadLocal = statefulHolderTransactionMap.get(currentTxGtrid);
        if (threadLocal != null) {
            XAStatefulHolder xaStatefulHolder = (XAStatefulHolder) threadLocal.get();
            // Additional sanity checks...
            if (xaStatefulHolder != null &&
                xaStatefulHolder.getState() != XAStatefulHolder.STATE_IN_POOL &&
                xaStatefulHolder.getState() != XAStatefulHolder.STATE_CLOSED) {

                if (log.isDebugEnabled()) { log.debug("sharing connection " + xaStatefulHolder + " in transaction " + currentTxGtrid); }
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
    private void putSharedXAStatefulHolder(final XAStatefulHolder xaStatefulHolder) {
        BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
        if (transaction == null) {
            if (log.isDebugEnabled()) { log.debug("no current transaction, not adding " + xaStatefulHolder + " to shared connection map"); }
            return;
        }
        final Uid currentTxGtrid = transaction.getResourceManager().getGtrid();

        StatefulHolderThreadLocal threadLocal = statefulHolderTransactionMap.get(currentTxGtrid);
        if (threadLocal == null) {
            // This is the first time this TxGtrid/ThreadLocal is going into the map,
            // register interest in synchronization so we can remove it at commit/rollback
            try {
                transaction.registerSynchronization(new SharedStatefulHolderCleanupSynchronization(currentTxGtrid));
            } catch (Exception e) {
                // OK, forget it.  The transaction is either rollback only or already finished.
                return;
            }

            threadLocal = new StatefulHolderThreadLocal();
            statefulHolderTransactionMap.put(currentTxGtrid, threadLocal);
            if (log.isDebugEnabled()) { log.debug("added shared connection mapping for " + currentTxGtrid + " holder " + xaStatefulHolder); }
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
                if (log.isDebugEnabled()) { log.debug("deleted shared connection mappings for " + gtrid); }
            }
        }

        public String toString() {
            return "a SharedStatefulHolderCleanupSynchronization with GTRID [" + gtrid + "]";
        }
    }

    private final class StatefulHolderThreadLocal extends ThreadLocal<XAStatefulHolder>
    {
    	@Override
    	public XAStatefulHolder get() {
    		return super.get();
    	}

    	@Override
    	public void set(XAStatefulHolder value) {
    		super.set(value);
    	}
    }
}
