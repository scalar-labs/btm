package bitronix.tm.resource.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixXid;
import bitronix.tm.recovery.IncrementalRecoverer;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.utils.CryptoEngine;
import bitronix.tm.utils.PropertyUtils;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.internal.*;

import javax.transaction.xa.XAResource;
import java.util.*;

/**
 * Generic XA pool. {@link XAStatefulHolder} instances are created by the {@link XAPool} out of a
 * {@link XAResourceProducer}. Those objects are then pooled and can be retrieved and/or recycled by the pool
 * depending on the running XA transaction's and the {@link XAStatefulHolder}'s states.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class XAPool implements StateChangeListener {

    private final static Logger log = LoggerFactory.getLogger(XAPool.class);
    private final static String PASSWORD_PROPERTY_NAME = "password";

    private List objects = new ArrayList();
    private ResourceBean bean;
    private XAResourceProducer xaResourceProducer;
    private Object xaFactory;
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
    }

    private void init() throws Exception {
        for (int i=0; i < bean.getMinPoolSize() ;i++) {
            createPooledObject(xaFactory);
        }

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
        long before = System.currentTimeMillis();
        while (true) {
            XAStatefulHolder xaStatefulHolder = null;
            String stateDescription = null;
            if (recycle) {
                xaStatefulHolder = getNotAccessible();
                stateDescription = "NOT_ACCESSIBLE";
            }
            if (xaStatefulHolder == null) {
                xaStatefulHolder = getInPool();
                stateDescription = "IN_POOL";
            }
            if (log.isDebugEnabled()) log.debug("found " + stateDescription + " connection " + xaStatefulHolder + " from " + this);

            try {
                return xaStatefulHolder.getConnectionHandle();
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
                try {
                    wait(bean.getAcquisitionInterval() * 1000L);
                } catch (InterruptedException ex2) {
                    // ignore
                }

                // check for timeout
                long now = System.currentTimeMillis();
                remainingTime -= (now - before);
                if (remainingTime <= 0) {
                    throw new BitronixRuntimeException("cannot get valid connection from " + this + " after trying for " + bean.getAcquisitionTimeout() + "s", ex);
                }
            }
        } // while true
    }

    public synchronized void close() {
        if (log.isDebugEnabled()) log.debug("closing all connections of " + this);
        for (int i = 0; i < totalPoolSize(); i++) {
            XAStatefulHolder xaStatefulHolder = (XAStatefulHolder) objects.get(i);
            try {
                xaStatefulHolder.close();
            } catch (Exception ex) {
                if (log.isDebugEnabled()) log.debug("ignoring catched exception while closing connection " + xaStatefulHolder, ex);
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
        for (int i = 0; i < totalPoolSize(); i++) {
            XAStatefulHolder xaStatefulHolder = (XAStatefulHolder) objects.get(i);
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
        for (int i = 0; i < totalPoolSize(); i++) {
            XAStatefulHolder xaStatefulHolder = (XAStatefulHolder) objects.get(i);
            List xaResourceHolders = xaStatefulHolder.getXAResourceHolders();

            for (int j = 0; j < xaResourceHolders.size(); j++) {
                XAResourceHolder holder = (XAResourceHolder) xaResourceHolders.get(j);
                if (holder.getXAResource() == xaResource)
                    return holder;
            }
        }
        return null;
    }

    public List getXAResourceHolders() {
        return objects;
    }

    public Date getNextShrinkDate() {
        return new Date(System.currentTimeMillis() + bean.getMaxIdleTime() * 1000);
    }

    public synchronized void shrink() throws Exception {
        if (log.isDebugEnabled()) log.debug("shrinking " + this);
        List toRemoveXaStatefulHolders = new ArrayList();
        long now = System.currentTimeMillis();
        for (int i = 0; i < totalPoolSize(); i++) {
            if (totalPoolSize() - toRemoveXaStatefulHolders.size() <= bean.getMinPoolSize()) {
                if (log.isDebugEnabled()) log.debug("pool reached min size");
                break;
            }

            XAStatefulHolder xaStatefulHolder = (XAStatefulHolder) objects.get(i);
            if (xaStatefulHolder.getState() != XAStatefulHolder.STATE_IN_POOL)
                continue;
            if (xaStatefulHolder.getLastReleaseDate() == null)
                continue;

            long expirationTime = (xaStatefulHolder.getLastReleaseDate().getTime() + (bean.getMaxIdleTime() * 1000));
            if (log.isDebugEnabled()) log.debug("checking if connection can be closed: " + xaStatefulHolder + " - closing time: " + expirationTime + ", now time: " + now);
            if (expirationTime <= now) {
                xaStatefulHolder.close();
                toRemoveXaStatefulHolders.add(xaStatefulHolder);
            }
        } // for
        if (log.isDebugEnabled()) log.debug("closed " + toRemoveXaStatefulHolders.size() + " idle connection(s)");
        objects.removeAll(toRemoveXaStatefulHolders);
    }

    public String toString() {
        return "an XAPool of resource " + bean.getUniqueName() + " with " + totalPoolSize() + " connection(s) (" + inPoolSize() + " still available)" + (failed ? " -failed-" : "");
    }

    private void createPooledObject(Object xaFactory) throws Exception {
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

        Iterator it = bean.getDriverProperties().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (name.endsWith(PASSWORD_PROPERTY_NAME)) {
                value = decrypt(value);
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

    private XAStatefulHolder getNotAccessible() {
        if (log.isDebugEnabled()) log.debug("trying to recycle a NOT_ACCESSIBLE connection of " + this);
        BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
        if (transaction == null) {
            if (log.isDebugEnabled()) log.debug("no current transaction, no connection can be in state NOT_ACCESSIBLE when there is no global transaction context");
            return null;
        }
        Uid currentTxGtrid = transaction.getResourceManager().getGtrid();
        if (log.isDebugEnabled()) log.debug("current transaction GTRID is [" + currentTxGtrid + "]");

        for (int i = 0; i < totalPoolSize(); i++) {
            XAStatefulHolder xaStatefulHolder = (XAStatefulHolder) objects.get(i);
            if (xaStatefulHolder.getState() == XAStatefulHolder.STATE_NOT_ACCESSIBLE) {
                if (log.isDebugEnabled()) log.debug("found a connection in NOT_ACCESSIBLE state: " + xaStatefulHolder);
                if (containsXAResourceHolderMatchingGtrid(xaStatefulHolder, currentTxGtrid))
                    return xaStatefulHolder;

            }
        } // for

        if (log.isDebugEnabled()) log.debug("no NOT_ACCESSIBLE connection enlisted in this transaction");
        return null;
    }

    private boolean containsXAResourceHolderMatchingGtrid(XAStatefulHolder xaStatefulHolder, Uid currentTxGtrid) {
        List xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
        if (log.isDebugEnabled()) log.debug(xaResourceHolders.size() + " xa resource(s) created by connection in NOT_ACCESSIBLE state: " + xaStatefulHolder);
        for (int i = 0; i < xaResourceHolders.size(); i++) {
            XAResourceHolder xaResourceHolder = (XAResourceHolder) xaResourceHolders.get(i);
            XAResourceHolderState xaResourceHolderState = xaResourceHolder.getXAResourceHolderState();
            if (xaResourceHolderState != null) {
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

    private XAStatefulHolder getInPool() throws Exception {
        if (log.isDebugEnabled()) log.debug("getting a IN_POOL connection from " + this);

        if (inPoolSize() == 0) {
            if (log.isDebugEnabled()) log.debug("no more free connection in " + this + ", trying to grow it");
            grow();
        }

        waitForConnectionInPool();
        for (int i = 0; i < totalPoolSize(); i++) {
            XAStatefulHolder xaStatefulHolder = (XAStatefulHolder) objects.get(i);
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
        }
        else {
            if (log.isDebugEnabled()) log.debug("pool " + bean.getUniqueName() + " already at max size of " + totalPoolSize() + " connection(s), not growing it");
        }
    }

    private synchronized void waitForConnectionInPool() throws Exception {
        long remainingTime = bean.getAcquisitionTimeout() * 1000L;
        if (log.isDebugEnabled()) log.debug("waiting for IN_POOL connections count to be > 0, currently is " + inPoolSize());
        while (inPoolSize() == 0) {
            long before = System.currentTimeMillis();
            try {
                if (log.isDebugEnabled()) log.debug("waiting " + remainingTime + "ms");
                wait(remainingTime);
                if (log.isDebugEnabled()) log.debug("waiting over, IN_POOL connections count is now " + inPoolSize());
            } catch (InterruptedException ex) {
                // ignore
            }

            long now = System.currentTimeMillis();
            remainingTime -= (now - before);
            if (remainingTime <= 0) {
                if (log.isDebugEnabled()) log.debug("connection pool dequeue timed out");
                if (TransactionManagerServices.isTransactionManagerRunning())
                    TransactionManagerServices.getTransactionManager().dumpTransactionContexts();
                throw new Exception("XA pool of resource " + bean.getUniqueName() + " still empty after " + bean.getAcquisitionTimeout() + "s wait time");
            }
        } // while
    }

}
