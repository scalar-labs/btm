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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.transaction.Synchronization;

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

    private final Map<Uid, StatefulHolderThreadLocal> statefulHolderTransactionMap = new ConcurrentHashMap<Uid, StatefulHolderThreadLocal>();
    private final BlockingQueue<XAStatefulHolder> availablePool = new LinkedBlockingQueue<XAStatefulHolder>();
    private final Queue<XAStatefulHolder> accessiblePool = new ConcurrentLinkedQueue<XAStatefulHolder>();
    private final Queue<XAStatefulHolder> inaccessiblePool = new ConcurrentLinkedQueue<XAStatefulHolder>();

    /**
     * The stateTransitionLock makes sure that transitions of XAStatefulHolders from one state to another
     * are atomic.  A ReentrantReadWriteLock allows any number of readers to access and iterate the 
     * accessiblePool and inaccessiblePool without blocking.  Readers are blocked only for the instant
     * when a connection is moving between pools.  We use a lock, in addition to the collections being 
     * concurrent, because concurrent collections only guarantee atomic reads and writes, but not consistent
     * iteration. Iterating over a concurrent collection while it is modified can lead to the same element
     * being seen twice or some elements never being seen at all.
     */
    private final ReentrantReadWriteLock stateTransitionLock = new ReentrantReadWriteLock();

    private final ResourceBean bean;
    private final XAResourceProducer xaResourceProducer;
    private final Object xaFactory;
    private final AtomicBoolean failed = new AtomicBoolean();

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

    /**
     * Get the XAFactory (XADataSource) that produces objects for this pool.
     *
     * @return the factory (XADataSource) object
     */
    public Object getXAFactory() {
        return xaFactory;
    }

    /**
     * Sets this XAPool as failed or unfailed, requiring recovery.
     *
     * @param failed true if this XAPool has failed and requires recovery, false if it is ok
     */
    public void setFailed(boolean failed) {
        this.failed.set(failed);
    }

    /**
     * Is the XAPool in a failed state?
     *
     * @return true if this XAPool has failed, false otherwise
     */
    public boolean isFailed() {
        return failed.get();
    }

    /**
     * Get a connection handle from this pool.
     *
     * @return a connection handle
     * @throws Exception throw in the pool is unrecoverable or a timeout occurs getting a connection
     */
    public Object getConnectionHandle() throws Exception {
        return getConnectionHandle(true);
    }
    
    /**
     * Get a connection handle from this pool.
     * 
     * @param recycle true if we should try to get a connection in the NON_ACCESSIBLE pool in the same transaction
     * @return a connection handle
     * @throws Exception throw in the pool is unrecoverable or a timeout occurs getting a connection
     */
    public Object getConnectionHandle(boolean recycle) throws Exception {
        if (isFailed()) {
            synchronized (this) {
                try {
                    if (isFailed()) {
                        if (log.isDebugEnabled()) { log.debug("resource '" + bean.getUniqueName() + "' is marked as failed, resetting and recovering it before trying connection acquisition"); }
                        close();
                        init();
                        IncrementalRecoverer.recover(xaResourceProducer);
                    }
                }
                catch (RecoveryException ex) {
                    throw new BitronixRuntimeException("incremental recovery failed when trying to acquire a connection from failed resource '" + bean.getUniqueName() + "'", ex);
                }
                catch (Exception ex) {
                    throw new BitronixRuntimeException("pool reset failed when trying to acquire a connection from failed resource '" + bean.getUniqueName() + "'", ex);
                }
            }
        }

        long remainingTimeMs = bean.getAcquisitionTimeout() * 1000L;
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
                xaStatefulHolder = getInPool(remainingTimeMs);
            }
            if (log.isDebugEnabled()) { log.debug("found " + Decoder.decodeXAStatefulHolderState(xaStatefulHolder.getState()) + " connection " + xaStatefulHolder + " from " + this); }

            try {
                // getConnectionHandle() here could throw an exception, if it doesn't the connection is
                // still alive and we can share it (if sharing is enabled)
                Object connectionHandle = xaStatefulHolder.getConnectionHandle();
                if (bean.getShareTransactionConnections()) {
                    putSharedXAStatefulHolder(xaStatefulHolder);
                }
                growUntilMinPoolSize();
                return connectionHandle;
            } catch (Exception ex) {
                try {
                    if (log.isDebugEnabled()) { log.debug("connection is invalid, trying to close it", ex); }
                    xaStatefulHolder.close();
                } catch (Exception ex2) {
                    if (log.isDebugEnabled()) { log.debug("exception while trying to close invalid connection, ignoring it", ex2); }
                }
                finally {
                    if (xaStatefulHolder.getState() != XAStatefulHolder.STATE_CLOSED) {
                        stateChanged(xaStatefulHolder, xaStatefulHolder.getState(), XAStatefulHolder.STATE_CLOSED);
                    }

                    if (log.isDebugEnabled()) { log.debug("removed invalid connection " + xaStatefulHolder + " from " + this); }

                    if (log.isDebugEnabled()) log.debug("waiting " + bean.getAcquisitionInterval() + "s before trying to acquire a connection again from " + this);
                    long waitTime = bean.getAcquisitionInterval() * 1000L;
                    if (waitTime > 0) {
                        try {
                            synchronized (this) {
                                wait(waitTime);
                            }
                        } catch (InterruptedException ex2) {
                            // ignore
                        }
                    }
                }

                // check for timeout
                long now = MonotonicClock.currentTimeMillis();
                remainingTimeMs -= (now - before);
                before = now;
                if (remainingTimeMs <= 0) {
                    throw new BitronixRuntimeException("cannot get valid connection from " + this + " after trying for " + bean.getAcquisitionTimeout() + "s", ex);
                }
            }
        } // while true
    }

    /**
     * Close down and cleanup this XAPool instance.
     */
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
        failed.set(false);
    }

    /**
     * Get the total size of this pool. 
     *
     * @return the total size of this pool
     */
    public int totalPoolSize() {
        return availablePool.size() + accessiblePool.size() + inaccessiblePool.size();
    }

    public int inPoolSize() {
    	return availablePool.size();
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

    /**
     * This method is called to initialize the pool.
     *
     * @throws Exception
     */
    private void init() throws Exception {
        growUntilMinPoolSize();

        if (bean.getMaxIdleTime() > 0 || bean.getMaxLifeTime() > 0) {
            TransactionManagerServices.getTaskScheduler().schedulePoolShrinking(this);
        }
    }

    public void shrink() throws Exception {
        if (log.isDebugEnabled()) { log.debug("shrinking " + this); }
        expireOrCloseStatefulHolders(false);
        if (log.isDebugEnabled()) { log.debug("shrunk " + this); }
    }

    public void reset() throws Exception {
        if (log.isDebugEnabled()) { log.debug("resetting " + this); }
        expireOrCloseStatefulHolders(true);
        if (log.isDebugEnabled()) { log.debug("reset " + this); }
    }

    private synchronized void expireOrCloseStatefulHolders(boolean forceClose) throws Exception {
        int closed = 0;
        final long now = MonotonicClock.currentTimeMillis();
        final int availableSize = availablePool.size();
        for (int i = 0; i < availableSize; i++) {
            XAStatefulHolder xaStatefulHolder = availablePool.poll();
            if (xaStatefulHolder == null) {
                break;
            }

            long expirationTime = Integer.MAX_VALUE;
            if (bean.getMaxIdleTime() > 0) {
                expirationTime = (xaStatefulHolder.getLastReleaseDate().getTime() + (bean.getMaxIdleTime() * 1000));
            }

            if (bean.getMaxLifeTime() > 0) {
                long endOfLife = xaStatefulHolder.getCreationDate().getTime() + (bean.getMaxLifeTime() * 1000);
                expirationTime = Math.min(expirationTime, endOfLife);
            }

            if (!forceClose && log.isDebugEnabled()) { log.debug("checking if connection can be closed: " + xaStatefulHolder + " - closing time: " + expirationTime + ", now time: " + now); }
            if (expirationTime <= now || forceClose) {
                try {
                    closed++;
                    xaStatefulHolder.close();
                } catch (Exception ex) {
                    log.warn("error closing " + xaStatefulHolder, ex);
                }
            } else {
                availablePool.add(xaStatefulHolder);
            }
        } // for

        if (log.isDebugEnabled()) { log.debug("closed " + closed + (forceClose ? " " : " idle ") + "connection(s)"); }

        growUntilMinPoolSize();
    }

    public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
        stateTransitionLock.writeLock().lock();
        try {
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
                source.removeStateChangeEventListener(this);
        		break;
        	}
        }
        finally {
            stateTransitionLock.writeLock().unlock();
        }
    }

    public void stateChanging(XAStatefulHolder source, int currentState, int futureState) {
        stateTransitionLock.writeLock().lock();
        try {
            switch (currentState) {
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
                source.removeStateChangeEventListener(this);
                break;
            }
        }
        finally {
            stateTransitionLock.writeLock().unlock();
        }
    }

    public String toString() {
        return "an XAPool of resource " + bean.getUniqueName() + " with " + totalPoolSize() + " connection(s) (" + inPoolSize() + " still available)" + (isFailed() ? " -failed-" : "");
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
            Object value = entry.getValue();

            if (name.endsWith(PASSWORD_PROPERTY_NAME)) {
                value = decrypt(value.toString());
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

    /**
     * Get a XAStatefulHolder (connection) from the NOT_ACCESSIBLE pool.
     *
     * @return a connection, or null if there are no connections in the inaccessible pool for the current transaction
     */
    private XAStatefulHolder getNotAccessible() {
        if (log.isDebugEnabled()) { log.debug("trying to recycle a NOT_ACCESSIBLE connection of " + this); }
        BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
        if (transaction == null) {
            if (log.isDebugEnabled()) { log.debug("no current transaction, no connection can be in state NOT_ACCESSIBLE when there is no global transaction context"); }
            return null;
        }
        Uid currentTxGtrid = transaction.getResourceManager().getGtrid();
        if (log.isDebugEnabled()) { log.debug("current transaction GTRID is [" + currentTxGtrid + "]"); }

        stateTransitionLock.readLock().lock();
        try {
            for (XAStatefulHolder xaStatefulHolder : inaccessiblePool) {
                if (log.isDebugEnabled()) { log.debug("found a connection in NOT_ACCESSIBLE state: " + xaStatefulHolder); }
                if (containsXAResourceHolderMatchingGtrid(xaStatefulHolder, currentTxGtrid))
                    return xaStatefulHolder;
            } // for
    
            if (log.isDebugEnabled()) { log.debug("no NOT_ACCESSIBLE connection enlisted in this transaction"); }
            return null;
        }
        finally {
            stateTransitionLock.readLock().unlock();
        }
    }

    private boolean containsXAResourceHolderMatchingGtrid(XAStatefulHolder xaStatefulHolder, final Uid currentTxGtrid) {
        List<XAResourceHolder> xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
        if (log.isDebugEnabled()) { log.debug(xaResourceHolders.size() + " xa resource(s) created by connection in NOT_ACCESSIBLE state: " + xaStatefulHolder); }
        for (XAResourceHolder xaResourceHolder : xaResourceHolders) {

            class LocalVisitor implements XAResourceHolderStateVisitor {
                private boolean found;
                public boolean visit(XAResourceHolderState xaResourceHolderState) {
                    // compare GTRIDs
                    BitronixXid bitronixXid = xaResourceHolderState.getXid();
                    Uid resourceGtrid = bitronixXid.getGlobalTransactionIdUid();
                    if (log.isDebugEnabled()) { log.debug("NOT_ACCESSIBLE xa resource GTRID: " + resourceGtrid); }
                    if (currentTxGtrid.equals(resourceGtrid)) {
                        if (log.isDebugEnabled()) { log.debug("NOT_ACCESSIBLE xa resource's GTRID matched this transaction's GTRID, recycling it"); }
                        found = true;
                    }
                    return !found; // continue visitation if not found, stop visitation if found
                }
            }
            LocalVisitor xaResourceHolderStateVisitor = new LocalVisitor();
            xaResourceHolder.acceptVisitorForXAResourceHolderStates(currentTxGtrid, xaResourceHolderStateVisitor);
            return xaResourceHolderStateVisitor.found;
        }
        return false;
    }

    /**
     * Get an IN_POOL connection.  This method blocks for up to remainingTimeMs milliseconds
     * for someone to return or create a connection in the available pool.  If remainingTimeMs
     * expires, an exception is thrown.
     *
     * @param remainingTimeMs the maximum time to wait for a connection
     * @return a connection from the available (IN_POOL) pool
     * @throws Exception thrown in no connection is available before the remainingTimeMs time expires
     */
    private XAStatefulHolder getInPool(long remainingTimeMs) throws Exception {
        if (log.isDebugEnabled()) { log.debug("getting a IN_POOL connection from " + this); }

        if (inPoolSize() == 0) {
            if (log.isDebugEnabled()) { log.debug("no more free connections in " + this + ", trying to grow it"); }
            grow();
        }

        if (log.isDebugEnabled()) { log.debug("getting IN_POOL connection, waiting if necessary, current size is " + inPoolSize()); }

        try {
        	XAStatefulHolder xaStatefulHolder = availablePool.poll(remainingTimeMs, TimeUnit.MILLISECONDS);
        	if (xaStatefulHolder == null) {
        		if (TransactionManagerServices.isTransactionManagerRunning())
        			TransactionManagerServices.getTransactionManager().dumpTransactionContexts();
        		
        		throw new BitronixRuntimeException("XA pool of resource " + bean.getUniqueName() + " still empty after " + bean.getAcquisitionTimeout() + "s wait time");
        	}

        	return xaStatefulHolder;
		} catch (InterruptedException e) {
			throw new BitronixRuntimeException("Interrupted while waiting for IN_POOL connection.");
		}
    }

    /**
     * Grow the pool by "acquire increment" amount up to the max pool size.
     *
     * @throws Exception thrown if creating a pooled objects fails
     */
    private synchronized void grow() throws Exception {
    	final long totalPoolSize = totalPoolSize();
        if (totalPoolSize < bean.getMaxPoolSize()) {
            long increment = bean.getAcquireIncrement();
            if (totalPoolSize + increment > bean.getMaxPoolSize()) {
                increment = bean.getMaxPoolSize() - totalPoolSize;
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

    private synchronized void growUntilMinPoolSize() throws Exception {
        if (log.isDebugEnabled()) { log.debug("growing " + this + " to minimum pool size " + bean.getMinPoolSize()); }
        for (int i = totalPoolSize(); i < bean.getMinPoolSize(); i++) {
            createPooledObject(xaFactory);
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
            statefulHolderTransactionMap.remove(gtrid);
            if (log.isDebugEnabled()) { log.debug("deleted shared connection mappings for " + gtrid); }
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
