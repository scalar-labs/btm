package bitronix.tm.resource.common;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.Scheduler;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.XAResourceHolderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class that contains static logic common accross all resource types.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TransactionContextHelper {

    private final static Logger log = LoggerFactory.getLogger(TransactionContextHelper.class);

    /**
     * Enlist the {@link XAResourceHolder} in the current transaction or do nothing if there is no global transaction
     * context for this thread.
     * @param xaResourceHolder the {@link XAResourceHolder} to enlist.
     * @param bean the {@link ResourceBean} of the {@link XAResourceHolder}.
     * @throws SystemException if an internal error happens.
     * @throws RollbackException if the current transaction has been marked as rollback only.
     */
    public static void enlistInCurrentTransaction(XAResourceHolder xaResourceHolder, ResourceBean bean) throws SystemException, RollbackException {
        BitronixTransaction currentTransaction = currentTransaction();
        if (log.isDebugEnabled()) log.debug("enlisting " + xaResourceHolder + " into " + currentTransaction);

        if (currentTransaction != null) {
            if (currentTransaction.timedOut())
                throw new BitronixSystemException("transaction timed out");

            XAResourceHolderState alreadyEnlistedXAResourceHolderState = TransactionContextHelper.getAlreadyEnlistedXAResourceHolderState(xaResourceHolder, currentTransaction);
            if (alreadyEnlistedXAResourceHolderState == null || alreadyEnlistedXAResourceHolderState.isEnded()) {
                enlist(xaResourceHolder, bean, currentTransaction);
            }
            else if (log.isDebugEnabled()) log.debug("avoiding re-enlistment of already enlisted but not ended resource " + alreadyEnlistedXAResourceHolderState);
        } // if currentTransaction != null
        else {
            if (bean.getAllowLocalTransactions()) {
                if (log.isDebugEnabled()) log.debug("in local transaction context, skipping enlistment");
            }
            else
                throw new BitronixSystemException("resource '" + bean.getUniqueName() + "' cannot be used outside XA " +
                        "transaction scope. Set allowLocalTransactions to true if you want to allow this and you know " +
                        "your resource supports this.");
        }
    }

    /**
     * Delist the {@link XAResourceHolder} from the current transaction or do nothing if there is no global transaction
     * context for this thread.
     * @param xaResourceHolder the {@link XAResourceHolder} to delist.
     * @param bean the {@link ResourceBean} of the {@link XAResourceHolder}.
     * @throws SystemException if an internal error happens.
     */
    public static void delistFromCurrentTransaction(XAResourceHolder xaResourceHolder, ResourceBean bean) throws SystemException {
        BitronixTransaction currentTransaction = currentTransaction();
        if (log.isDebugEnabled()) log.debug("delisting " + xaResourceHolder + " from " + currentTransaction);

        // End resource as eagerly as possible. This allows to release connections to the pool much earlier
        // with resources fully supporting transaction interleaving.
        if (isInEnlistingGlobalTransactionContext(xaResourceHolder, currentTransaction) && !bean.getDeferConnectionRelease()) {
            XAResourceHolderState xaResourceHolderState = xaResourceHolder.getXAResourceHolderState();
            if (!xaResourceHolderState.isEnded()) {
                if (log.isDebugEnabled()) log.debug("delisting resource " + xaResourceHolderState + " from " + currentTransaction);

                // Watch out: the delistResource() call might throw a BitronixRollbackSystemException to indicate a unilateral rollback.
                // Is there something that should be done here in this regard ?
                currentTransaction.delistResource(xaResourceHolderState.getXAResource(), XAResource.TMSUCCESS);
            }
            else if (log.isDebugEnabled()) log.debug("avoiding delistment of not enlisted resource " + xaResourceHolderState);
        } // isInEnlistingGlobalTransactionContext
    }

    /**
     * Get the transaction running on the current thead context.
     * @return null if there is no transaction on the current context or if the transaction manager is not running.
     */
    public static BitronixTransaction currentTransaction() {
        if (!TransactionManagerServices.isTransactionManagerRunning())
            return null;
        return TransactionManagerServices.getTransactionManager().getCurrentTransaction();
    }

    /**
     * Switch the {@link XAStatefulHolder}'s state appropriately after the acquired resource handle has been closed.
     * The pooled resource will either be marked as closed or not accessible, depending on the value of the bean's
     * <code>deferConnectionRelease</code> property and will be marked for release after 2PC execution in the latter case.
     * @param xaStatefulHolder the {@link XAStatefulHolder} to requeue.
     * @param bean the {@link ResourceBean} of the {@link XAResourceHolder}.
     * @throws BitronixSystemException if an internal error happens.
     */
    public static void requeue(XAStatefulHolder xaStatefulHolder, ResourceBean bean) throws BitronixSystemException {
        BitronixTransaction currentTransaction = currentTransaction();
        if (log.isDebugEnabled()) log.debug("requeuing " + xaStatefulHolder + " from " + currentTransaction);

        if (!TransactionContextHelper.isInEnlistingGlobalTransactionContext(xaStatefulHolder, currentTransaction)) {
            if (!TransactionContextHelper.isEnlistedInSomeTransaction(xaStatefulHolder)) {
                // local mode, always requeue connection immediately
                if (log.isDebugEnabled()) log.debug("resource not in enlisting global transaction context, immediately releasing to pool " + xaStatefulHolder);
                xaStatefulHolder.setState(XAResourceHolder.STATE_IN_POOL);
            } else {
                throw new BitronixSystemException("cannot close a resource when its XAResource is taking part in an unfinished global transaction");
            }
        }
        else if (bean.getDeferConnectionRelease()) {
            // global mode, defer connection requeuing
            if (log.isDebugEnabled()) log.debug("deferring release to pool of " + xaStatefulHolder);

            if (!TransactionContextHelper.isAlreadyRegisteredForDeferredRelease(xaStatefulHolder, currentTransaction)) {
                if (log.isDebugEnabled()) log.debug("registering DeferredReleaseSynchronization for " + xaStatefulHolder);
                DeferredReleaseSynchronization synchronization = new DeferredReleaseSynchronization(xaStatefulHolder);
                currentTransaction.getSynchronizationScheduler().add(synchronization, Scheduler.ALWAYS_LAST_POSITION);
            }
            else if (log.isDebugEnabled()) log.debug("already registered DeferredReleaseSynchronization for " + xaStatefulHolder);

            xaStatefulHolder.setState(XAResourceHolder.STATE_NOT_ACCESSIBLE);
        }
        else {
            // global mode, immediate connection requeuing
            if (log.isDebugEnabled()) log.debug("immediately releasing to pool " + xaStatefulHolder);
            xaStatefulHolder.setState(XAResourceHolder.STATE_IN_POOL);
        }
    }

    /**
     * Ensure the {@link XAStatefulHolder}'s release won't be deferred anymore (when appropriate) as it has been recycled.
     * @param xaStatefulHolder the recycled {@link XAStatefulHolder}.
     */
    public static void recycle(XAStatefulHolder xaStatefulHolder) {
        BitronixTransaction currentTransaction = currentTransaction();
        if (log.isDebugEnabled()) log.debug("marking " + xaStatefulHolder + " as recycled in " + currentTransaction);
        Scheduler synchronizationScheduler = currentTransaction.getSynchronizationScheduler();

        DeferredReleaseSynchronization deferredReleaseSynchronization = findDeferredRelease(xaStatefulHolder, currentTransaction);
        if (deferredReleaseSynchronization != null) {
            if (log.isDebugEnabled()) log.debug(xaStatefulHolder + " has been recycled, unregistering deferred release from " + currentTransaction);
            synchronizationScheduler.remove(deferredReleaseSynchronization);
        }
    }


    /* private methods must not call TransactionManagerServices.getTransactionManager().getCurrentTransaction() */

    private static boolean isAlreadyRegisteredForDeferredRelease(XAStatefulHolder xaStatefulHolder, BitronixTransaction currentTransaction) {
        boolean alreadyDeferred = findDeferredRelease(xaStatefulHolder, currentTransaction) != null;
        if (log.isDebugEnabled()) log.debug(xaStatefulHolder + " is " + (alreadyDeferred ? "" : "not ") + "already registered for deferred release in " + currentTransaction);
        return alreadyDeferred;
    }

    private static DeferredReleaseSynchronization findDeferredRelease(XAStatefulHolder xaStatefulHolder, BitronixTransaction currentTransaction) {
        Scheduler synchronizationScheduler = currentTransaction.getSynchronizationScheduler();
        Iterator it = synchronizationScheduler.iterator();

        while (it.hasNext()) {
            Synchronization synchronization = (Synchronization) it.next();
            if (synchronization instanceof DeferredReleaseSynchronization) {
                DeferredReleaseSynchronization deferredReleaseSynchronization = (DeferredReleaseSynchronization) synchronization;
                if (deferredReleaseSynchronization.getXAStatefulHolder() == xaStatefulHolder) {
                    return deferredReleaseSynchronization;
                }
            } // if synchronization instanceof DeferredReleaseSynchronization
        } // for

        return null;
    }

    private static boolean isEnlistedInSomeTransaction(XAResourceHolder xaResourceHolder) throws BitronixSystemException {
        if (log.isDebugEnabled()) log.debug("looking in in-flight transactions for XAResourceHolderState of " + xaResourceHolder);

        if (!TransactionManagerServices.isTransactionManagerRunning()) {
            if (log.isDebugEnabled()) log.debug("transaction manager not running, there is no in-flight transaction");
            return false;
        }

        return xaResourceHolder.hasStateForXAResource(xaResourceHolder);
    }

    private static boolean isEnlistedInSomeTransaction(XAStatefulHolder xaStatefulHolder) throws BitronixSystemException {
        List xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
        if (xaResourceHolders == null)
            return false;

        for (int i = 0; i < xaResourceHolders.size(); i++) {
            XAResourceHolder xaResourceHolder = (XAResourceHolder) xaResourceHolders.get(i);
            boolean enlisted = isEnlistedInSomeTransaction(xaResourceHolder);
            if (enlisted)
                return true;
        }

        return false;
    }


    private static boolean isInEnlistingGlobalTransactionContext(XAResourceHolder xaResourceHolder, BitronixTransaction currentTransaction) {
        boolean globalTransactionMode = false;
        if (xaResourceHolder.getXAResourceHolderState() != null && currentTransaction != null) {
            Uid currentTxGtrid = currentTransaction.getResourceManager().getGtrid();
            if (log.isDebugEnabled()) log.debug("current transaction GTRID is [" + currentTxGtrid + "]");
            BitronixXid bitronixXid = xaResourceHolder.getXAResourceHolderState().getXid();
            Uid resourceGtrid = bitronixXid.getGlobalTransactionIdUid();
            if (log.isDebugEnabled()) log.debug("resource GTRID is [" + resourceGtrid + "]");
            globalTransactionMode = currentTxGtrid.equals(resourceGtrid);
        }
        if (log.isDebugEnabled()) log.debug("resource is " + (globalTransactionMode ? "" : "not ") + "in enlisting global transaction context: " + xaResourceHolder);
        return globalTransactionMode;
    }

    private static boolean isInEnlistingGlobalTransactionContext(XAStatefulHolder xaStatefulHolder, BitronixTransaction currentTransaction) {
        List xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
        if (xaResourceHolders == null)
            return false;

        for (int i = 0; i < xaResourceHolders.size(); i++) {
            XAResourceHolder xaResourceHolder = (XAResourceHolder) xaResourceHolders.get(i);
            boolean enlisted = isInEnlistingGlobalTransactionContext(xaResourceHolder, currentTransaction);
            if (enlisted)
                return true;
        }

        return false;
    }

    private static XAResourceHolderState getAlreadyEnlistedXAResourceHolderState(XAResourceHolder xaResourceHolder, BitronixTransaction currentTransaction) {
        XAResourceHolderState xaResourceHolderState = xaResourceHolder.getXAResourceHolderState();
        if (xaResourceHolderState != null && xaResourceHolderState.getXid() != null && currentTransaction != null) {
            BitronixXid bitronixXid = xaResourceHolderState.getXid();
            Uid resourceGtrid = bitronixXid.getGlobalTransactionIdUid();
            Uid currentTransactionGtrid = currentTransaction.getResourceManager().getGtrid();
            if (currentTransactionGtrid.equals(resourceGtrid))
                return xaResourceHolderState;
        }
        return null;
    }

    private static void enlist(XAResourceHolder xaResourceHolder, ResourceBean bean, BitronixTransaction currentTransaction) throws RollbackException, SystemException {
        try {
            XAResourceHolderState xaResourceHolderState = new XAResourceHolderState(xaResourceHolder, bean);
            if (log.isDebugEnabled()) log.debug("enlisting resource " + xaResourceHolderState + " into " + currentTransaction);
            xaResourceHolder.setXAResourceHolderState(xaResourceHolderState);
            currentTransaction.enlistResource(xaResourceHolderState.getXAResource());
        }
        catch (RollbackException e) {
            xaResourceHolder.setXAResourceHolderState(null);
            throw e;
        }
        catch (IllegalStateException e) {
            xaResourceHolder.setXAResourceHolderState(null);
            throw e;
        }
        catch (SystemException e) {
            xaResourceHolder.setXAResourceHolderState(null);
            throw e;
        }
    }

}
