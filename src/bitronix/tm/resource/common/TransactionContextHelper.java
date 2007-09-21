package bitronix.tm.resource.common;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.Uid;
import bitronix.tm.internal.XAResourceHolderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helper class that contains static logic common accross all resource types.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
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
     * @throws SystemException
     * @throws RollbackException
     */
    public static void enlistInCurrentTransaction(XAResourceHolder xaResourceHolder, ResourceBean bean) throws SystemException, RollbackException {
        BitronixTransaction currentTransaction = TransactionManagerServices.getTransactionManager().getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("enlisting " + xaResourceHolder + " into " + currentTransaction);

        if (currentTransaction != null) {
            if (currentTransaction.timedOut())
                throw new BitronixSystemException("transaction timed out");

            XAResourceHolderState alreadyEnlistedXAResourceHolderState = TransactionContextHelper.getAlreadyEnlistedXAResourceHolderState(xaResourceHolder, currentTransaction);
            if (alreadyEnlistedXAResourceHolderState == null || alreadyEnlistedXAResourceHolderState.isEnded()) {
                XAResourceHolderState xaResourceHolderState = new XAResourceHolderState(xaResourceHolder, bean);
                if (log.isDebugEnabled()) log.debug("enlisting resource " + xaResourceHolderState + " into " + currentTransaction);
                xaResourceHolder.setXAResourceHolderState(xaResourceHolderState);
                currentTransaction.enlistResource(xaResourceHolderState.getXAResource());
            } else if (log.isDebugEnabled()) log.debug("avoiding re-enlistment of already enlisted but not ended resource " + alreadyEnlistedXAResourceHolderState);
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
     * @throws SystemException
     */
    public static void delistFromCurrentTransaction(XAResourceHolder xaResourceHolder, ResourceBean bean) throws SystemException {
        BitronixTransaction currentTransaction = TransactionManagerServices.getTransactionManager().getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("delisting " + xaResourceHolder + " from " + currentTransaction);

        // End resource as eagerly as possible. This allows to release connections to the pool much earlier
        // with resources fully supporting transaction interleaving.
        if (isInEnlistingGlobalTransactionContext(xaResourceHolder, currentTransaction) && !bean.getDeferConnectionRelease()) {
            XAResourceHolderState xaResourceHolderState = xaResourceHolder.getXAResourceHolderState();
            if (!xaResourceHolderState.isEnded()) {
                if (log.isDebugEnabled()) log.debug("delisting resource " + xaResourceHolderState + " from " + currentTransaction);
                currentTransaction.delistResource(xaResourceHolderState.getXAResource(), XAResource.TMSUCCESS);
            }
            else if (log.isDebugEnabled()) log.debug("avoiding delistment of not enlisted resource " + xaResourceHolderState);
        } // isInEnlistingGlobalTransactionContext
    }

    /**
     * Switches the {@link XAResourceHolder}'s state appropriately after the acquired resource handle has been closed.
     * The pooled resource will either be marked as closed or not accessible, depending on the value of the bean's
     * <code>deferConnectionRelease</code> property and will be marked for release after 2PC execution in the latter case.
     * @param xaResourceHolder the {@link XAResourceHolder} to requeue.
     * @param bean the {@link ResourceBean} of the {@link XAResourceHolder}.
     * @throws BitronixSystemException
     */
    public static void requeue(XAResourceHolder xaResourceHolder, ResourceBean bean) throws BitronixSystemException {
        BitronixTransaction currentTransaction = TransactionManagerServices.getTransactionManager().getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("requeuing " + xaResourceHolder + " from " + currentTransaction);

        if (!TransactionContextHelper.isInEnlistingGlobalTransactionContext(xaResourceHolder, currentTransaction)) {
            if (!TransactionContextHelper.isEnlistedInSomeTransaction(xaResourceHolder)) {
                // local mode, always requeue connection immediately
                if (log.isDebugEnabled()) log.debug("not in enlisting global transaction context, immediately releasing to pool " + xaResourceHolder);
                xaResourceHolder.setState(XAResourceHolder.STATE_IN_POOL);
            } else {
                throw new BitronixSystemException("cannot close a resource when its XAResource is taking part in an unfinished global transaction");
            }
        } else if (bean.getDeferConnectionRelease()) {
            // global mode, defer connection requeuing
            if (log.isDebugEnabled()) log.debug("deferring release to pool of " + xaResourceHolder);
            xaResourceHolder.setState(XAResourceHolder.STATE_NOT_ACCESSIBLE);

            if (!TransactionContextHelper.isAlreadyRegisteredForDeferredRelease(xaResourceHolder, currentTransaction)) {
                if (log.isDebugEnabled()) log.debug("registering deferred release synchronization for " + xaResourceHolder);
                DeferredReleaseSynchronization synchronization = new DeferredReleaseSynchronization(xaResourceHolder);
                currentTransaction.getSynchronizations().add(synchronization);
            }
            else if (log.isDebugEnabled()) log.debug("already registered deferred release synchronization for " + xaResourceHolder);
        } else {
            // global mode, immediate connection requeuing
            if (log.isDebugEnabled()) log.debug("immediately releasing to pool " + xaResourceHolder);
            xaResourceHolder.setState(XAResourceHolder.STATE_IN_POOL);
        }
    }

    public static void requeue(XAStatefulHolder xaStatefulHolder, ResourceBean bean) throws BitronixSystemException {
        BitronixTransaction currentTransaction = TransactionManagerServices.getTransactionManager().getCurrentTransaction();
        if (log.isDebugEnabled()) log.debug("requeuing " + xaStatefulHolder + " from " + currentTransaction);

        if (!TransactionContextHelper.isInEnlistingGlobalTransactionContext(xaStatefulHolder, currentTransaction)) {
            if (!TransactionContextHelper.isEnlistedInSomeTransaction(xaStatefulHolder)) {
                // local mode, always requeue connection immediately
                if (log.isDebugEnabled()) log.debug("not resource in enlisting global transaction context, immediately releasing to pool " + xaStatefulHolder);
                xaStatefulHolder.setState(XAResourceHolder.STATE_IN_POOL);
            } else {
                throw new BitronixSystemException("cannot close a resource producer when one of its XAResource's is taking part in an unfinished global transaction");
            }
        } else if (bean.getDeferConnectionRelease()) {
            // global mode, defer connection requeuing
            if (log.isDebugEnabled()) log.debug("deferring release to pool of " + xaStatefulHolder);
            xaStatefulHolder.setState(XAResourceHolder.STATE_NOT_ACCESSIBLE);

            if (!TransactionContextHelper.isAlreadyRegisteredForDeferredRelease(xaStatefulHolder, currentTransaction)) {
                if (log.isDebugEnabled()) log.debug("registering DeferredReleaseSynchronization for " + xaStatefulHolder);
                DeferredReleaseSynchronization synchronization = new DeferredReleaseSynchronization(xaStatefulHolder);
                currentTransaction.getSynchronizations().add(synchronization);
            }
            else if (log.isDebugEnabled()) log.debug("already registered DeferredReleaseSynchronization for " + xaStatefulHolder);
        } else {
            // global mode, immediate connection requeuing
            if (log.isDebugEnabled()) log.debug("immediately releasing to pool " + xaStatefulHolder);
            xaStatefulHolder.setState(XAResourceHolder.STATE_IN_POOL);
        }
    }

    /* private methods must not call TransactionManagerServices.getTransactionManager().getCurrentTransaction() */

    private static boolean isAlreadyRegisteredForDeferredRelease(XAStatefulHolder xaStatefulHolder, BitronixTransaction currentTransaction) {
        boolean alreadyDeferred = false;
        List synchronizations = currentTransaction.getSynchronizations();
        for (int i = 0; i < synchronizations.size(); i++) {
            Synchronization synchronization = (Synchronization) synchronizations.get(i);
            if (synchronization instanceof DeferredReleaseSynchronization) {
                DeferredReleaseSynchronization deferredReleaseSynchronization = (DeferredReleaseSynchronization) synchronization;
                if (deferredReleaseSynchronization.getXAStatefulHolder() == xaStatefulHolder) {
                    alreadyDeferred = true;
                    break;
                }
            } // if synchronization instanceof DeferredReleaseSynchronization
        } // for

        if (log.isDebugEnabled()) log.debug(xaStatefulHolder + " is " + (alreadyDeferred ? "" : "not ") + "already registered for deferred release in " + currentTransaction);
        return alreadyDeferred;
    }

    private static boolean isEnlistedInSomeTransaction(XAResourceHolder xaResourceHolder) throws BitronixSystemException {
        if (log.isDebugEnabled()) log.debug("looking in in-flight transactions for XAResourceHolderState of " + xaResourceHolder);

        Map inFlights = TransactionManagerServices.getTransactionManager().getInFlightTransactions();
        Iterator it = inFlights.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            BitronixTransaction tx = (BitronixTransaction) entry.getValue();

            XAResourceHolderState holder = tx.getResourceManager().findXAResourceHolderState(xaResourceHolder.getXAResource());
            if (holder != null) {
                if (log.isDebugEnabled()) log.debug("resource " + xaResourceHolder + " is enlisted in " + tx);
                return true;
            }
        }

        if (log.isDebugEnabled()) log.debug("resource not enlisted in any transaction: " + xaResourceHolder);
        return false;
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
            BitronixXid bitronixXid = (BitronixXid) xaResourceHolder.getXAResourceHolderState().getXid();
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
            BitronixXid bitronixXid = (BitronixXid) xaResourceHolderState.getXid();
            Uid resourceGtrid = bitronixXid.getGlobalTransactionIdUid();
            Uid currentTransactionGtrid = currentTransaction.getResourceManager().getGtrid();
            if (currentTransactionGtrid.equals(resourceGtrid))
                return xaResourceHolderState;
        }
        return null;
    }

    public static void markRecycled(XAStatefulHolder xaStatefulHolder) {
        BitronixTransaction currentTransaction = TransactionManagerServices.getTransactionManager().getCurrentTransaction();
        List synchronizations = currentTransaction.getSynchronizations();
        for (int i = 0; i < synchronizations.size(); i++) {
            Synchronization synchronization = (Synchronization) synchronizations.get(i);
            if (synchronization instanceof DeferredReleaseSynchronization) {
                DeferredReleaseSynchronization deferredReleaseSynchronization = (DeferredReleaseSynchronization) synchronization;
                if (deferredReleaseSynchronization.getXAStatefulHolder() == xaStatefulHolder) {
                    if (log.isDebugEnabled()) log.debug(xaStatefulHolder + " has been recycled, unregistering deferred release from " + currentTransaction);
                    synchronizations.remove(deferredReleaseSynchronization);
                    break;
                }
            } // if synchronization instanceof DeferredReleaseSynchronization
            log.warn("cannot unregister DeferredReleaseSynchronization of " + xaStatefulHolder + " from " + currentTransaction);
        } // for

    }
}
