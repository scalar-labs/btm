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

import bitronix.tm.internal.BitronixMultiSystemException;
import bitronix.tm.internal.BitronixRollbackException;
import bitronix.tm.internal.BitronixRollbackSystemException;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.BitronixXAException;
import bitronix.tm.internal.TransactionStatusChangeListener;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.internal.XAResourceManager;
import bitronix.tm.journal.Journal;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.timer.TaskScheduler;
import bitronix.tm.twopc.Committer;
import bitronix.tm.twopc.PhaseException;
import bitronix.tm.twopc.Preparer;
import bitronix.tm.twopc.Rollbacker;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.ExceptionUtils;
import bitronix.tm.utils.ManagementRegistrar;
import bitronix.tm.utils.MonotonicClock;
import bitronix.tm.utils.Scheduler;
import bitronix.tm.utils.StackTrace;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link Transaction}.
 *
 * @author lorban
 */
public class BitronixTransaction implements Transaction, BitronixTransactionMBean {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransaction.class);

    private final XAResourceManager resourceManager;
    private final Scheduler<Synchronization> synchronizationScheduler = new Scheduler<Synchronization>();
    private final List<TransactionStatusChangeListener> transactionStatusListeners = new ArrayList<TransactionStatusChangeListener>();
    private volatile int status = Status.STATUS_NO_TRANSACTION;
    private volatile boolean timeout = false;
    private volatile Date timeoutDate;

    private final Executor executor = TransactionManagerServices.getExecutor();
    private final TaskScheduler taskScheduler = TransactionManagerServices.getTaskScheduler();
    private final Journal journal = TransactionManagerServices.getJournal();

    private final Preparer preparer = new Preparer(executor);
    private final Committer committer = new Committer(executor);
    private final Rollbacker rollbacker = new Rollbacker(executor);

    /* management */
    private volatile String threadName;
    private volatile Date startDate;
    private volatile StackTrace activationStackTrace;


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
            String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer().extractExtraXAExceptionDetails(ex);
            if (BitronixXAException.isUnilateralRollback(ex)) {
                // if the resource unilaterally rolled back, the transaction will never be able to commit -> mark it as rollback only
                setStatus(Status.STATUS_MARKED_ROLLBACK);
                throw new BitronixRollbackException("resource " + resourceHolderState + " unilaterally rolled back, error=" +
                        Decoder.decodeXAExceptionErrorCode(ex) + (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
            }
            throw new BitronixSystemException("cannot enlist " + resourceHolderState + ", error=" +
                    Decoder.decodeXAExceptionErrorCode(ex) + (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
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

        Map<Uid, XAResourceHolderState> statesForGtrid = resourceHolder.getXAResourceHolderStatesForGtrid(resourceManager.getGtrid());

        boolean result = true;
        List<Exception> exceptions = new ArrayList<Exception>();
        List<XAResourceHolderState> resourceStates = new ArrayList<XAResourceHolderState>();
        for (XAResourceHolderState resourceHolderState : statesForGtrid.values()) {
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

            String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer().extractExtraXAExceptionDetails(ex);
            if (BitronixXAException.isUnilateralRollback(ex)) {
                // The resource unilaterally rolled back here. We have to throw an exception to indicate this but
                // The signature of this method is inherited from javax.transaction.Transaction. Thereof, we have choice
                // between creating a sub-exception of SystemException or using a RuntimeException. Is that the best way
                // forward as this 'hidden' exception can be left throw out at unexpected locations where SystemException
                // should be rethrown but the exception thrown here should be catched & handled... ?
                throw new BitronixRollbackSystemException("resource " + resourceHolderState + " unilaterally rolled back, error=" +
                        Decoder.decodeXAExceptionErrorCode(ex) + (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
            }
            throw new BitronixSystemException("cannot delist " + resourceHolderState + ", error=" + Decoder.decodeXAExceptionErrorCode(ex) +
                    (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
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

    public Scheduler<Synchronization> getSynchronizationScheduler() {
        return synchronizationScheduler;
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction hasn't started yet");
        if (isDone())
            throw new IllegalStateException("transaction is done, cannot commit it");

        taskScheduler.cancelTransactionTimeout(this);

        // beforeCompletion must be called before the check to STATUS_MARKED_ROLLBACK as the synchronization
        // can still set the status to STATUS_MARKED_ROLLBACK.
        try {
            fireBeforeCompletionEvent();
        } catch (BitronixSystemException ex) {
            rollback();
            throw new BitronixRollbackException("SystemException thrown during beforeCompletion cycle caused transaction rollback", ex);
        } catch (RuntimeException ex) {
            rollback();
            throw new BitronixRollbackException("RuntimeException thrown during beforeCompletion cycle caused transaction rollback", ex);
        }

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
            // the caught BitronixRollbackException's message is pre-formatted to be appended to this message
            throw new BitronixRollbackException("delistment error caused transaction rollback" + ex.getMessage());
        }

        if (status == Status.STATUS_MARKED_ROLLBACK) {
            if (log.isDebugEnabled()) log.debug("transaction marked as rollback only");
            rollback();
            throw new BitronixRollbackException("transaction was marked as rollback only and has been rolled back");
        }

        try {
            List<XAResourceHolderState> interestedResources;

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

            if (resourceManager.size() == 0 && TransactionManagerServices.getConfiguration().isDebugZeroResourceTransaction()) {
                log.warn(buildZeroTransactionDebugMessage(activationStackTrace, new StackTrace()));
            }

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

        taskScheduler.cancelTransactionTimeout(this);

        try {
            delistUnclosedResources(XAResource.TMSUCCESS);
        } catch (BitronixRollbackException ex) {
            if (log.isDebugEnabled()) log.debug("some resource(s) failed delistment", ex);
        }

        try {
            try {
                if (log.isDebugEnabled()) log.debug("rolling back, " + resourceManager.size() + " enlisted resource(s)");

                List<XAResourceHolderState> resourcesToRollback = new ArrayList<XAResourceHolderState>();
                List<XAResourceHolderState> allResources = resourceManager.getAllResources();
                for (XAResourceHolderState resource : allResources) {
                    if (!resource.isFailed())
                        resourcesToRollback.add(resource);
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
        if (TransactionManagerServices.getConfiguration().isDebugZeroResourceTransaction()) {
            this.activationStackTrace = new StackTrace();
        }

        taskScheduler.scheduleTransactionTimeout(this, timeoutDate);
    }


    public void setStatus(int status) throws BitronixSystemException {
        setStatus(status, resourceManager.collectUniqueNames());
    }

    public void setStatus(int status, Set<String> uniqueNames) throws BitronixSystemException {
        try {
            boolean force = (resourceManager.size() > 1) && (status == Status.STATUS_COMMITTING);
            if (log.isDebugEnabled()) log.debug("changing transaction status to " + Decoder.decodeStatus(status) + (force ? " (forced)" : ""));

            int oldStatus = this.status;
            this.status = status;
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

        for (TransactionStatusChangeListener listener : transactionStatusListeners) {
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
        List<XAResourceHolderState> allResources = resourceManager.getAllResources();
        List<XAResourceHolderState> rolledBackResources = new ArrayList<XAResourceHolderState>();
        List<XAResourceHolderState> failedResources = new ArrayList<XAResourceHolderState>();

        for (XAResourceHolderState resource : allResources) {
            if (!resource.isEnded()) {
                if (log.isDebugEnabled()) log.debug("found unclosed resource to delist: " + resource);
                try {
                    delistResource(resource, flag);
                } catch (BitronixRollbackSystemException ex) {
                    rolledBackResources.add(resource);
                    if (log.isDebugEnabled())
                        log.debug("resource unilaterally rolled back: " + resource, ex);
                } catch (SystemException ex) {
                    failedResources.add(resource);
                    log.warn("error delisting resource, assuming unilateral rollback: " + resource, ex);
                }
            } else if (log.isDebugEnabled())
                log.debug("no need to delist already closed resource: " + resource);
        } // for

        if (!rolledBackResources.isEmpty() || !failedResources.isEmpty()) {
            String lineSeparator = System.getProperty("line.separator");
            StringBuilder sb = new StringBuilder();
            if (!rolledBackResources.isEmpty()) {
                sb.append(lineSeparator);
                sb.append("  resource(s) ");
                sb.append(Decoder.collectResourcesNames(rolledBackResources));
                sb.append(" unilaterally rolled back");

            }
            if (!failedResources.isEmpty()) {
                sb.append(lineSeparator);
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
        List<XAResourceHolderState> interestedResources = resourceManager.getAllResources();
        try {
            rollbacker.rollback(this, interestedResources);
            if (log.isDebugEnabled()) log.debug("rollback after prepare failure succeeded");
        } catch (Exception ex) {
            // let's merge both exceptions' PhaseException to report a complete error message
            PhaseException preparePhaseEx = (PhaseException) rbEx.getCause();
            PhaseException rollbackPhaseEx = (PhaseException) ex.getCause();

            List<Exception> exceptions = new ArrayList<Exception>();
            List<XAResourceHolderState> resources = new ArrayList<XAResourceHolderState>();

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
        for (Synchronization synchronization : synchronizationScheduler) {
            try {
                if (log.isDebugEnabled()) log.debug("executing synchronization " + synchronization + " with status=" + Decoder.decodeStatus(status));
                synchronization.afterCompletion(status);
            } catch (Exception ex) {
                log.warn("Synchronization.afterCompletion() call failed for " + synchronization, ex);
            }
        }

        ManagementRegistrar.unregister("bitronix.tm:type=Transaction,Gtrid=" + resourceManager.getGtrid());
    }

    static String buildZeroTransactionDebugMessage(StackTrace activationStackTrace, StackTrace commitStackTrace) {
        String lineSeparator = System.getProperty("line.separator");
        final StringBuilder sb = new StringBuilder();
        sb.append("committed transaction with 0 enlisted resource").append(lineSeparator);
        sb.append("==================== Began at ====================").append(lineSeparator);
        sb.append(ExceptionUtils.getStackTrace(activationStackTrace)).append(lineSeparator);
        sb.append("==================== Committed at ====================").append(lineSeparator);
        sb.append(ExceptionUtils.getStackTrace(commitStackTrace)).append(lineSeparator);
        return sb.toString();
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

    /**
     * Returns the activation {@link StackTrace} if it is available.
     *
     * @return the call stack of where the transaction began
     */
    StackTrace getActivationStackTrace() {
        return activationStackTrace;
    }
}
