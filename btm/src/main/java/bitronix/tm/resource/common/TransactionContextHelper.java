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

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Scheduler;
import bitronix.tm.utils.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.util.List;
import java.util.Map;

/**
 * Helper class that contains static logic common accross all resource types.
 *
 * @author lorban
 */
public class TransactionContextHelper {

    private final static Logger log = LoggerFactory.getLogger(TransactionContextHelper.class);

    /**
     * Enlist the {@link XAResourceHolder} in the current transaction or do nothing if there is no global transaction
     * context for this thread.
     * @param xaResourceHolder the {@link XAResourceHolder} to enlist.
     * @throws SystemException if an internal error happens.
     * @throws RollbackException if the current transaction has been marked as rollback only.
     */
    public static void enlistInCurrentTransaction(XAResourceHolder xaResourceHolder) throws SystemException, RollbackException {
        BitronixTransaction currentTransaction = currentTransaction();
        ResourceBean bean = xaResourceHolder.getResourceBean();
        if (log.isDebugEnabled()) log.debug("enlisting " + xaResourceHolder + " into " + currentTransaction);

        if (currentTransaction != null) {
            if (currentTransaction.timedOut())
                throw new BitronixSystemException("transaction timed out");

            // in case multiple unjoined branches of the current transaction have run on the resource,
            // only the last one counts as all the first ones are ended already
            XAResourceHolderState alreadyEnlistedXAResourceHolderState = TransactionContextHelper.getLatestAlreadyEnlistedXAResourceHolderState(xaResourceHolder, currentTransaction);
            if (alreadyEnlistedXAResourceHolderState == null || alreadyEnlistedXAResourceHolderState.isEnded()) {
                currentTransaction.enlistResource(xaResourceHolder.getXAResource());
            }
            else if (log.isDebugEnabled()) log.debug("avoiding re-enlistment of already enlisted but not ended resource " + alreadyEnlistedXAResourceHolderState);
        }
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
     * @throws SystemException if an internal error happens.
     */
    public static void delistFromCurrentTransaction(XAResourceHolder xaResourceHolder) throws SystemException {
        BitronixTransaction currentTransaction = currentTransaction();
        ResourceBean bean = xaResourceHolder.getResourceBean();
        if (log.isDebugEnabled()) log.debug("delisting " + xaResourceHolder + " from " + currentTransaction);

        // End resource as eagerly as possible. This allows to release connections to the pool much earlier
        // with resources fully supporting transaction interleaving.
        if (isInEnlistingGlobalTransactionContext(xaResourceHolder, currentTransaction) && !bean.getDeferConnectionRelease()) {
            Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolder.getXAResourceHolderStatesForGtrid(currentTransaction.getResourceManager().getGtrid());
            for (XAResourceHolderState xaResourceHolderState : statesForGtrid.values()) {
                if (!xaResourceHolderState.isEnded()) {
                    if (log.isDebugEnabled())
                        log.debug("delisting resource " + xaResourceHolderState + " from " + currentTransaction);

                    // Watch out: the delistResource() call might throw a BitronixRollbackSystemException to indicate a unilateral rollback.
                    currentTransaction.delistResource(xaResourceHolderState.getXAResource(), XAResource.TMSUCCESS);
                } else if (log.isDebugEnabled()) log.debug("avoiding delistment of not enlisted resource " + xaResourceHolderState);
            }

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
        Scheduler<Synchronization> synchronizationScheduler = currentTransaction.getSynchronizationScheduler();

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
        Scheduler<Synchronization> synchronizationScheduler = currentTransaction.getSynchronizationScheduler();

        for (Synchronization synchronization : synchronizationScheduler) {
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
        List<XAResourceHolder> xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
        if (xaResourceHolders == null)
            return false;

        for (XAResourceHolder xaResourceHolder : xaResourceHolders) {
            boolean enlisted = isEnlistedInSomeTransaction(xaResourceHolder);
            if (enlisted)
                return true;
        }

        return false;
    }


    private static boolean isInEnlistingGlobalTransactionContext(XAResourceHolder xaResourceHolder, BitronixTransaction currentTransaction) {
        boolean globalTransactionMode = false;
        if (currentTransaction != null && xaResourceHolder.getXAResourceHolderStatesForGtrid(currentTransaction.getResourceManager().getGtrid()) != null) {
            globalTransactionMode = true;
        }
        if (log.isDebugEnabled()) log.debug("resource is " + (globalTransactionMode ? "" : "not ") + "in enlisting global transaction context: " + xaResourceHolder);
        return globalTransactionMode;
    }

    private static boolean isInEnlistingGlobalTransactionContext(XAStatefulHolder xaStatefulHolder, BitronixTransaction currentTransaction) {
        List<XAResourceHolder> xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
        if (xaResourceHolders == null)
            return false;

        for (XAResourceHolder xaResourceHolder : xaResourceHolders) {
            boolean enlisted = isInEnlistingGlobalTransactionContext(xaResourceHolder, currentTransaction);
            if (enlisted)
                return true;
        }

        return false;
    }

    private static XAResourceHolderState getLatestAlreadyEnlistedXAResourceHolderState(XAResourceHolder xaResourceHolder, BitronixTransaction currentTransaction) {
        if (currentTransaction == null)
            return null;
        Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolder.getXAResourceHolderStatesForGtrid(currentTransaction.getResourceManager().getGtrid());
        if (statesForGtrid == null)
            return null;

        XAResourceHolderState result = null;

        // iteration order is guraranteed so just take the latest matching one in the iterator
        for (XAResourceHolderState xaResourceHolderState : statesForGtrid.values()) {
            if (xaResourceHolderState != null && xaResourceHolderState.getXid() != null) {
                BitronixXid bitronixXid = xaResourceHolderState.getXid();
                Uid resourceGtrid = bitronixXid.getGlobalTransactionIdUid();
                Uid currentTransactionGtrid = currentTransaction.getResourceManager().getGtrid();

                if (currentTransactionGtrid.equals(resourceGtrid)) {
                    result = xaResourceHolderState;
                }
            }
        }

        return result;
    }

}
