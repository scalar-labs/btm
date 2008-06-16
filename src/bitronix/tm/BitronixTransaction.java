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
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixTransaction implements Transaction, BitronixTransactionMBean {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransaction.class);

    private int status = Status.STATUS_NO_TRANSACTION;
    private XAResourceManager resourceManager;
    private Scheduler synchronizationScheduler = new Scheduler();
    private boolean timeout = false;
    private Date timeoutDate;

    private Preparer preparer = new Preparer(TransactionManagerServices.getExecutor());
    private Committer committer = new Committer(TransactionManagerServices.getExecutor());
    private Rollbacker rollbacker = new Rollbacker(TransactionManagerServices.getExecutor());

    /* management */
    private String threadName;
    private Date startDate;


    public BitronixTransaction(int timeout) {
        Uid gtrid = UidGenerator.generateUid();
        if (log.isDebugEnabled()) log.debug("creating new transaction with GTRID [" + gtrid + "]");
        this.resourceManager = new XAResourceManager(gtrid);

        this.threadName = Thread.currentThread().getName();
        this.startDate = new Date();
        this.timeoutDate = new Date(System.currentTimeMillis() + (timeout * 1000L));

        TransactionManagerServices.getTaskScheduler().scheduleTransactionTimeout(this, timeoutDate);
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
        XAResourceHolderState holder = resourceHolder.getXAResourceHolderState();

        try {
            resourceManager.enlist(holder);
        } catch (XAException ex) {
            throw new BitronixSystemException("cannot enlist " + holder + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
        }

        return true;
    }

    public boolean delistResource(XAResource xaResource, int flag) throws IllegalStateException, SystemException {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction hasn't started yet");
        if (flag != XAResource.TMSUCCESS && flag != XAResource.TMSUSPEND && flag != XAResource.TMFAIL)
            throw new BitronixSystemException("can only delist with SUCCESS, SUSPEND, FAIL - was: " + Decoder.decodeXAResourceFlag(flag));
        if (isWorking())
            throw new IllegalStateException("transaction is being committed or rolled back, cannot delist any resource now");

        XAResourceHolder XAResourceHolder = ResourceRegistrar.findXAResourceHolder(xaResource);
        if (XAResourceHolder == null)
            throw new BitronixSystemException("unknown XAResource " + xaResource + ", it does not belong to a registered resource");
        XAResourceHolderState holder = XAResourceHolder.getXAResourceHolderState();

        try {
            return resourceManager.delist(holder, flag);
        } catch (XAException ex) {
            throw new BitronixSystemException("cannot delist " + holder + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
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

        fireBeforeCompletionEvent();
        TransactionManagerServices.getTaskScheduler().cancelTransactionTimeout(this);

        if (timedOut()) {
            if (log.isDebugEnabled()) log.debug("transaction timed out");
            rollback();
            throw new BitronixRollbackException("transaction timed out and has been rolled back");
        }
        if (status == Status.STATUS_MARKED_ROLLBACK) {
            if (log.isDebugEnabled()) log.debug("transaction marked as rollback only");
            rollback();
            throw new BitronixRollbackException("transaction was marked as rollback only and has been rolled back");
        }

        delistUnclosedResources(XAResource.TMSUCCESS);

        List interestedResources = new ArrayList();
        try {
            if (log.isDebugEnabled()) log.debug("committing, " + resourceManager.size() + " enlisted resource(s)");

            preparer.prepare(this, interestedResources);
            if (log.isDebugEnabled()) log.debug(interestedResources.size() + " interested resource(s)");
            committer.commit(this, interestedResources);

            if (log.isDebugEnabled()) log.debug("successfully committed " + toString());
        }
        catch (RollbackException ex) {
            if (log.isDebugEnabled()) log.debug("caught rollback exception during prepare, trying to rollback");

            // rollbackPreparedResources might throw a SystemException that will 'swallow' the RollbackException which is
            // what we want in that case as the transaction has not been rolled back and some resources are now left in-doubt.
            rollbackPreparedResources(interestedResources, ex);

            throw ex;
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
        delistUnclosedResources(XAResource.TMSUCCESS);

        try {
            if (log.isDebugEnabled()) log.debug("rolling back, " + resourceManager.size() + " enlisted resource(s)");

            rollbacker.rollback(this, resourceManager.getAllResources());

            if (log.isDebugEnabled()) log.debug("successfully rolled back " + toString());
        } catch (HeuristicMixedException ex) {
            throw new BitronixSystemException("transaction partly committed and partly rolled back. Resources are now inconsistent !", ex);
        } catch (HeuristicCommitException ex) {
            throw new BitronixSystemException("transaction committed instead of rolled back. Resources are now inconsistent !", ex);
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
    }

    public boolean timedOut() {
        return timeout;
    }

    public Date getTimeoutDate() {
        return timeoutDate;
    }

    public void setActive() throws IllegalStateException, SystemException {
        if (status != Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction has already started");
        setStatus(Status.STATUS_ACTIVE);
    }

    public void setStatus(int status) throws BitronixSystemException {
        try {
            boolean force = (resourceManager.size() > 1) && (status == Status.STATUS_COMMITTING);
            if (log.isDebugEnabled()) log.debug("changing transaction status to " + Decoder.decodeStatus(status) + (force ? " (forced)" : ""));

            this.status = status;
            Journal journal = TransactionManagerServices.getJournal();
            journal.log(status, resourceManager.getGtrid(), resourceManager.collectUniqueNames());
            if (force) {
                journal.force();
            }

            if (status == Status.STATUS_ACTIVE)
                ManagementRegistrar.register("bitronix.tm:type=Transaction,Gtrid=" + resourceManager.getGtrid(), this);
        } catch (IOException ex) {
            // if we cannot log, the TM must stop managing TX until the problem is fixed
            throw new BitronixSystemException("error logging status", ex);
        }
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
     */
    private void delistUnclosedResources(int flag) {
        List resources = resourceManager.getAllResources();
        for (int i = 0; i < resources.size(); i++) {
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) resources.get(i);
            if (!xaResourceHolderState.isEnded()) {
                if (log.isDebugEnabled()) log.debug("found unclosed resource to delist: " + xaResourceHolderState);
                try {
                    delistResource(xaResourceHolderState.getXAResource(), flag);
                } catch (SystemException ex) {
                    log.warn("error delisting resource: " + xaResourceHolderState, ex);
                }
            }
            else
                if (log.isDebugEnabled()) log.debug("no need to delist already closed resource: " + xaResourceHolderState);
        } // for
    }

    /**
     * Rollback resources that have been prepared after a phase 1 prepare failure.
     * @param interestedResources resources to be rolled back.
     * @param rbEx the thrown rollback exception.
     * @throws BitronixSystemException when a resource could not rollback prepapared state.
     */
    private void rollbackPreparedResources(List interestedResources, RollbackException rbEx) throws BitronixSystemException {
        try {
            rollbacker.rollback(this, interestedResources);
            if (log.isDebugEnabled()) log.debug("rollback of prepared resources succeeded");
        } catch (Exception ex) {
            // let's merge both exceptions' PhaseException to report a complete error message
            PhaseException preparePhaseEx = (PhaseException) rbEx.getCause();
            PhaseException rollbackPhaseEx = (PhaseException) ex.getCause();

            List exceptions = new ArrayList();
            List resources = new ArrayList();

            exceptions.addAll(preparePhaseEx.getExceptions());
            exceptions.addAll(rollbackPhaseEx.getExceptions());
            resources.addAll(preparePhaseEx.getResources());
            resources.addAll(rollbackPhaseEx.getResources());

            throw new BitronixSystemException("transaction partially prepared and only partially rolled back. Some resources are left in doubt !", new PhaseException(exceptions, resources));
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
        Iterator it = synchronizationScheduler.iterator();
        while (it.hasNext()) {
            Synchronization synchronization = (Synchronization) it.next();
            try {
                if (log.isDebugEnabled()) log.debug("executing synchronization " + synchronization);
                synchronization.beforeCompletion();
            } catch (RuntimeException ex) {
                log.warn("Synchronization.beforeCompletion() call failed for " + synchronization + ", marking transaction as rollback only", ex);
                setStatus(Status.STATUS_MARKED_ROLLBACK);
                return;
            }
        }
    }

    private void fireAfterCompletionEvent() {
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
