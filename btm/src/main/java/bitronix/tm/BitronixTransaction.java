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

import bitronix.tm.internal.*;
import bitronix.tm.journal.Journal;
import bitronix.tm.twopc.*;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.utils.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.transaction.*;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.util.*;

/**
 * Implementation of {@link Transaction}.
 *
 * @author lorban
 */
public class BitronixTransaction implements Transaction, BitronixTransactionMBean {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransaction.class);

    private volatile int status = Status.STATUS_NO_TRANSACTION;
    private XAResourceManager resourceManager;
    private Scheduler synchronizationScheduler = new Scheduler();
    private List transactionStatusListeners = new ArrayList();
    private boolean timeout = false;
    private Date timeoutDate;

    private Preparer preparer = new Preparer(TransactionManagerServices.getExecutor());
    private Committer committer = new Committer(TransactionManagerServices.getExecutor());
    private Rollbacker rollbacker = new Rollbacker(TransactionManagerServices.getExecutor());

    /* management */
    private String threadName;
    private Date startDate;


    public BitronixTransaction() {
        Uid gtrid = UidGenerator.generateUid();
        if (log.isDebugEnabled()) log.debug("creating new transaction with GTRID [" + gtrid + "]");
        this.resourceManager = new XAResourceManager(gtrid);

        this.threadName = Thread.currentThread().getName();
    }

    public int getStatus() throws SystemException {
        return status;
    }

    public boolean enlistResource(XAResource xaResource) throws RollbackException, IllegalStateException, SystemException {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction hasn't started yet");
        if (status == Status.STATUS_MARKED_ROLLBACK)
            throw new BitronixRollbackException("transaction has been marked as rollback only");
        if (isDone())
            throw new IllegalStateException("transaction started or finished 2PC, cannot enlist any more resource");

        XAResourceHolder resourceHolder = ResourceRegistrar.findXAResourceHolder(xaResource);
        if (resourceHolder == null)
            throw new BitronixSystemException("unknown XAResource " + xaResource + ", it does not belong to a registered resource");

        XAResourceHolderState resourceHolderState = new XAResourceHolderState(resourceHolder, resourceHolder.getResourceBean());

        // resource timeout must be set here so manually enlisted resources can receive it
        resourceHolderState.setTransactionTimeoutDate(timeoutDate);

        try {
            resourceManager.enlist(resourceHolderState);
        } catch (XAException ex) {
            if (BitronixXAException.isUnilateralRollback(ex)) {
                // if the resource unilaterally rolled back, the transaction will never be able to commit -> mark it as rollback only
                setStatus(Status.STATUS_MARKED_ROLLBACK);
                throw new BitronixRollbackException("resource " + resourceHolderState + " unilaterally rolled back, error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
            }
            throw new BitronixSystemException("cannot enlist " + resourceHolderState + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
        }

        resourceHolder.putXAResourceHolderState(resourceHolderState.getXid(), resourceHolderState);
        return true;
    }

    public boolean delistResource(XAResource xaResource, int flag) throws IllegalStateException, SystemException {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction hasn't started yet");
        if (flag != XAResource.TMSUCCESS && flag != XAResource.TMSUSPEND && flag != XAResource.TMFAIL)
            throw new BitronixSystemException("can only delist with SUCCESS, SUSPEND, FAIL - was: " + Decoder.decodeXAResourceFlag(flag));
        if (isWorking())
            throw new IllegalStateException("transaction is being committed or rolled back, cannot delist any resource now");

        XAResourceHolder resourceHolder = ResourceRegistrar.findXAResourceHolder(xaResource);
        if (resourceHolder == null)
            throw new BitronixSystemException("unknown XAResource " + xaResource + ", it does not belong to a registered resource");

        Map statesForGtrid = resourceHolder.getXAResourceHolderStatesForGtrid(resourceManager.getGtrid());
        Iterator statesForGtridIt = statesForGtrid.values().iterator();

        boolean result = false;
        List exceptions = new ArrayList();
        List resourceStates = new ArrayList();
        while (statesForGtridIt.hasNext()) {
            XAResourceHolderState resourceHolderState = (XAResourceHolderState) statesForGtridIt.next();
            try {
                result &= delistResource(resourceHolderState, flag);
            } catch (BitronixSystemException ex) {
                if (log.isDebugEnabled()) log.debug("failed to delist resource state " + resourceHolderState);
                exceptions.add(ex);
                resourceStates.add(resourceHolderState);
            }
        }
        if (!exceptions.isEmpty()) {
            BitronixMultiSystemException multiSystemException = new BitronixMultiSystemException("error delisting resource", exceptions, resourceStates);
            if (!multiSystemException.isUnilateralRollback())
                throw multiSystemException;
            else
                if (log.isDebugEnabled()) log.debug("unilateral rollback of resource " + resourceHolder, multiSystemException);
        }

        return result;
    }

    private boolean delistResource(XAResourceHolderState resourceHolderState, int flag) throws BitronixSystemException {
        try {
           return resourceManager.delist(resourceHolderState, flag);
        }
        catch (XAException ex) {
            // if the resource could not be delisted, the transaction must not commit -> mark it as rollback only
            if (status != Status.STATUS_MARKED_ROLLBACK)
                setStatus(Status.STATUS_MARKED_ROLLBACK);

            if (BitronixXAException.isUnilateralRollback(ex)) {
                // The resource unilaterally rolled back here. We have to throw an exception to indicate this but
                // The signature of this method is inherited from javax.transaction.Transaction. Thereof, we have choice
                // between creating a sub-exception of SystemException or using a RuntimeException. Is that the best way
                // forward as this 'hidden' exception can be left throw out at unexpected locations where SystemException
                // should be rethrown but the exception thrown here should be catched & handled... ?
                throw new BitronixRollbackSystemException("resource " + resourceHolderState + " unilaterally rolled back, error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
            }
            throw new BitronixSystemException("cannot delist " + resourceHolderState + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
        }
    }

    public void registerSynchronization(Synchronization synchronization) throws RollbackException, IllegalStateException, SystemException {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction hasn't started yet");
        if (status == Status.STATUS_MARKED_ROLLBACK)
            throw new BitronixRollbackException("transaction has been marked as rollback only");
        if (isDone())
            throw new IllegalStateException("transaction is done, cannot register any more synchronization");

        if (log.isDebugEnabled()) log.debug("registering synchronization " + synchronization);
        synchronizationScheduler.add(synchronization, Scheduler.DEFAULT_POSITION);
    }

    public Scheduler getSynchronizationScheduler() {
        return synchronizationScheduler;
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction hasn't started yet");
        if (isDone())
            throw new IllegalStateException("transaction is done, cannot commit it");

        TransactionManagerServices.getTaskScheduler().cancelTransactionTimeout(this);

        // beforeCompletion must be called before the check to STATUS_MARKED_ROLLBACK as the synchronization
        // can still set the status to STATUS_MARKED_ROLLBACK.
        fireBeforeCompletionEvent();

        // The following if statements and try/catch block must not be included in the prepare try-catch block as
        // they call rollback().
        // Doing so would call fireAfterCompletionEvent() twice in case one of those conditions are true.
        if (timedOut()) {
            if (log.isDebugEnabled()) log.debug("transaction timed out");
            rollback();
            throw new BitronixRollbackException("transaction timed out and has been rolled back");
        }

        try {
            delistUnclosedResources(XAResource.TMSUCCESS);
        } catch (BitronixRollbackException ex) {
            if (log.isDebugEnabled()) log.debug("delistment error causing transaction rollback", ex);
            rollback();
            throw new BitronixRollbackException("delistment error caused transaction rollback" + ex.getMessage());
        }

        if (status == Status.STATUS_MARKED_ROLLBACK) {
            if (log.isDebugEnabled()) log.debug("transaction marked as rollback only");
            rollback();
            throw new BitronixRollbackException("transaction was marked as rollback only and has been rolled back");
        }

        try {
            List interestedResources;

            // prepare phase
            try {
                if (log.isDebugEnabled()) log.debug("committing, " + resourceManager.size() + " enlisted resource(s)");

                interestedResources = preparer.prepare(this);
            }
            catch (RollbackException ex) {
                if (log.isDebugEnabled()) log.debug("caught rollback exception during prepare, trying to rollback");

                // rollbackPrepareFailure might throw a SystemException that will 'swallow' the RollbackException which is
                // what we want in that case as the transaction has not been rolled back and some resources are now left in-doubt.
                rollbackPrepareFailure(ex);
                throw new BitronixRollbackException("transaction failed to prepare: " + this, ex);
            }

            // commit phase
            if (log.isDebugEnabled()) log.debug(interestedResources.size() + " interested resource(s)");

            committer.commit(this, interestedResources);

            if (log.isDebugEnabled()) log.debug("successfully committed " + this);
        }
        finally {
            fireAfterCompletionEvent();
        }
    }

    public void rollback() throws IllegalStateException, SystemException {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction hasn't started yet");
        if (isDone())
            throw new IllegalStateException("transaction is done, cannot roll it back");

        TransactionManagerServices.getTaskScheduler().cancelTransactionTimeout(this);

        try {
            delistUnclosedResources(XAResource.TMSUCCESS);
        } catch (BitronixRollbackException ex) {
            if (log.isDebugEnabled()) log.debug("some resource(s) failed delistment", ex);
        }

        try {
            try {
                if (log.isDebugEnabled()) log.debug("rolling back, " + resourceManager.size() + " enlisted resource(s)");

                List resourcesToRollback = new ArrayList();
                List allResources = resourceManager.getAllResources();
                for (int i = 0; i < allResources.size(); i++) {
                    XAResourceHolderState resourceHolderState = (XAResourceHolderState) allResources.get(i);
                    if (!resourceHolderState.isFailed())
                        resourcesToRollback.add(resourceHolderState);
                }

                rollbacker.rollback(this, resourcesToRollback);

                if (log.isDebugEnabled()) log.debug("successfully rolled back " + this);
            } catch (HeuristicMixedException ex) {
                throw new BitronixSystemException("transaction partly committed and partly rolled back. Resources are now inconsistent !", ex);
            } catch (HeuristicCommitException ex) {
                throw new BitronixSystemException("transaction committed instead of rolled back. Resources are now inconsistent !", ex);
            }
        } finally {
            fireAfterCompletionEvent();
        }
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction hasn't started yet");
        if (isDone())
            throw new IllegalStateException("transaction is done, cannot change its status");

        setStatus(Status.STATUS_MARKED_ROLLBACK);
    }

    public XAResourceManager getResourceManager() {
        return resourceManager;
    }

    public void timeout() throws BitronixSystemException {
        this.timeout = true;
        setStatus(Status.STATUS_MARKED_ROLLBACK);
        log.warn("transaction timed out: " + this);
    }

    public boolean timedOut() {
        return timeout;
    }

    public void setActive(int timeout) throws IllegalStateException, SystemException {
        if (status != Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction has already started");

        setStatus(Status.STATUS_ACTIVE);
        this.startDate = new Date(MonotonicClock.currentTimeMillis());
        this.timeoutDate = new Date(MonotonicClock.currentTimeMillis() + (timeout * 1000L));

        TransactionManagerServices.getTaskScheduler().scheduleTransactionTimeout(this, timeoutDate);
    }


    public void setStatus(int status) throws BitronixSystemException {
        setStatus(status, resourceManager.collectUniqueNames());
    }

    public void setStatus(int status, Set uniqueNames) throws BitronixSystemException {
        try {
            boolean force = (resourceManager.size() > 1) && (status == Status.STATUS_COMMITTING);
            if (log.isDebugEnabled()) log.debug("changing transaction status to " + Decoder.decodeStatus(status) + (force ? " (forced)" : ""));

            int oldStatus = this.status;
            this.status = status;
            Journal journal = TransactionManagerServices.getJournal();
            journal.log(status, resourceManager.getGtrid(), uniqueNames);
            if (force) {
                journal.force();
            }

            if (status == Status.STATUS_ACTIVE)
                ManagementRegistrar.register("bitronix.tm:type=Transaction,Gtrid=" + resourceManager.getGtrid(), this);

            fireTransactionStatusChangedEvent(oldStatus, status);
        } catch (IOException ex) {
            // if we cannot log, the TM must stop managing TX until the problem is fixed
            throw new BitronixSystemException("error logging status", ex);
        }
    }

    private void fireTransactionStatusChangedEvent(int oldStatus, int newStatus) {
        if (log.isDebugEnabled()) log.debug("transaction status is changing from " + Decoder.decodeStatus(oldStatus) + " to " +
                Decoder.decodeStatus(newStatus) + " - executing " + transactionStatusListeners.size() + " listener(s)");
        
        for (int i = 0; i < transactionStatusListeners.size(); i++) {
            TransactionStatusChangeListener listener = (TransactionStatusChangeListener) transactionStatusListeners.get(i);
            if (log.isDebugEnabled()) log.debug("executing TransactionStatusChangeListener " + listener);
            listener.statusChanged(oldStatus, newStatus);
            if (log.isDebugEnabled()) log.debug("executed TransactionStatusChangeListener " + listener);
        }
    }

    public void addTransactionStatusChangeListener(TransactionStatusChangeListener listener) {
        transactionStatusListeners.add(listener);
    }

    public int hashCode() {
        return resourceManager.getGtrid().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof BitronixTransaction) {
            BitronixTransaction tx = (BitronixTransaction) obj;
            return resourceManager.getGtrid().equals(tx.resourceManager.getGtrid());
        }
        return false;
    }

    public String toString() {
        return "a Bitronix Transaction with GTRID [" + resourceManager.getGtrid() + "], status=" + Decoder.decodeStatus(status) + ", " + resourceManager.size() + " resource(s) enlisted (started " + startDate + ")";
    }


    /*
    * Internal impl
    */


    /**
     * Delist all resources that have not been closed before calling tm.commit(). This basically means calling
     * XAResource.end() on all resource that has not been ended yet.
     * @param flag the flag to pass to XAResource.end(). Either TMSUCCESS or TMFAIL.
     * @throws bitronix.tm.internal.BitronixRollbackException if some resources unilaterally rolled back before end() call.
     */
    private void delistUnclosedResources(int flag) throws BitronixRollbackException {
        List resources = resourceManager.getAllResources();
        List rolledBackResources = new ArrayList();
        List failedResources = new ArrayList();

        for (int i = 0; i < resources.size(); i++) {
            XAResourceHolderState resourceHolderState = (XAResourceHolderState) resources.get(i);
            if (!resourceHolderState.isEnded()) {
                if (log.isDebugEnabled()) log.debug("found unclosed resource to delist: " + resourceHolderState);
                try {
                    delistResource(resourceHolderState, flag);
                } catch (BitronixRollbackSystemException ex) {
                    rolledBackResources.add(resourceHolderState);
                    if (log.isDebugEnabled()) log.debug("resource unilaterally rolled back: " + resourceHolderState, ex);
                } catch (SystemException ex) {
                    failedResources.add(resourceHolderState);
                    log.warn("error delisting resource, assuming unilateral rollback: " + resourceHolderState, ex);
                }
            }
            else
                if (log.isDebugEnabled()) log.debug("no need to delist already closed resource: " + resourceHolderState);
        } // for

        if (!rolledBackResources.isEmpty() || !failedResources.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            if (!rolledBackResources.isEmpty()) {
                sb.append(System.getProperty("line.separator"));
                sb.append("  resource(s) ");
                sb.append(Decoder.collectResourcesNames(rolledBackResources));
                sb.append(" unilaterally rolled back");

            }
            if (!failedResources.isEmpty()) {
                sb.append(System.getProperty("line.separator"));
                sb.append("  resource(s) ");
                sb.append(Decoder.collectResourcesNames(failedResources));
                sb.append(" could not be delisted");

            }

            throw new BitronixRollbackException(sb.toString());
        }
    }

    /**
     * Rollback resources after a phase 1 prepare failure. All resources must be rolled back as prepared ones
     * are in-doubt and non-prepared ones have started/ended work done that must also be cleaned.
     * @param rbEx the thrown rollback exception.
     * @throws BitronixSystemException when a resource could not rollback prepapared state.
     */
    private void rollbackPrepareFailure(RollbackException rbEx) throws BitronixSystemException {
        List interestedResources = resourceManager.getAllResources();
        try {
            rollbacker.rollback(this, interestedResources);
            if (log.isDebugEnabled()) log.debug("rollback after prepare failure succeeded");
        } catch (Exception ex) {
            // let's merge both exceptions' PhaseException to report a complete error message
            PhaseException preparePhaseEx = (PhaseException) rbEx.getCause();
            PhaseException rollbackPhaseEx = (PhaseException) ex.getCause();

            List exceptions = new ArrayList();
            List resources = new ArrayList();

            exceptions.addAll(preparePhaseEx.getExceptions());
            exceptions.addAll(rollbackPhaseEx.getExceptions());
            resources.addAll(preparePhaseEx.getResourceStates());
            resources.addAll(rollbackPhaseEx.getResourceStates());

            throw new BitronixSystemException("transaction partially prepared and only partially rolled back. Some resources might be left in doubt!", new PhaseException(exceptions, resources));
        }
    }

    /**
     * Run all registered Synchronizations' beforeCompletion() method. Be aware that this method can change the
     * transaction status to mark it as rollback only for instance.
     * @throws bitronix.tm.internal.BitronixSystemException if status changing due to a synchronization throwing an
     *         exception fails.
     */
    private void fireBeforeCompletionEvent() throws BitronixSystemException {
        if (log.isDebugEnabled()) log.debug("before completion, " + synchronizationScheduler.size() + " synchronization(s) to execute");
        Iterator it = synchronizationScheduler.reverseIterator();
        while (it.hasNext()) {
            Synchronization synchronization = (Synchronization) it.next();
            try {
                if (log.isDebugEnabled()) log.debug("executing synchronization " + synchronization);
                synchronization.beforeCompletion();
            } catch (RuntimeException ex) {
                if (log.isDebugEnabled()) log.debug("Synchronization.beforeCompletion() call failed for " + synchronization + ", marking transaction as rollback only - " + ex);
                setStatus(Status.STATUS_MARKED_ROLLBACK);
                throw ex;
            }
        }
    }

    private void fireAfterCompletionEvent() {
        // this TX is no longer in-flight -> remove this transaction's state from all XAResourceHolders
        getResourceManager().clearXAResourceHolderStates();

        if (log.isDebugEnabled()) log.debug("after completion, " + synchronizationScheduler.size() + " synchronization(s) to execute");
        Iterator it = synchronizationScheduler.iterator();
        while (it.hasNext()) {
            Synchronization synchronization = (Synchronization) it.next();
            try {
                if (log.isDebugEnabled()) log.debug("executing synchronization " + synchronization + " with status=" + Decoder.decodeStatus(status));
                synchronization.afterCompletion(status);
            } catch (Exception ex) {
                log.warn("Synchronization.afterCompletion() call failed for " + synchronization, ex);
            }
        }

        ManagementRegistrar.unregister("bitronix.tm:type=Transaction,Gtrid=" + resourceManager.getGtrid());
    }

    private boolean isDone() {
        switch (status) {
            case Status.STATUS_PREPARING:
            case Status.STATUS_PREPARED:
            case Status.STATUS_COMMITTING:
            case Status.STATUS_COMMITTED:
            case Status.STATUS_ROLLING_BACK:
            case Status.STATUS_ROLLEDBACK:
                return true;
        }
        return false;
    }

    private boolean isWorking() {
        switch (status) {
            case Status.STATUS_PREPARING:
            case Status.STATUS_PREPARED:
            case Status.STATUS_COMMITTING:
            case Status.STATUS_ROLLING_BACK:
                return true;
        }
        return false;
    }

    /* management */

    public String getGtrid() {
        return resourceManager.getGtrid().toString();
    }

    public String getStatusDescription() {
        return Decoder.decodeStatus(status);
    }

    public Collection getEnlistedResourcesUniqueNames() {
        return resourceManager.collectUniqueNames();
    }

    public String getThreadName() {
        return threadName;
    }

    public Date getStartDate() {
        return startDate;
    }
}
