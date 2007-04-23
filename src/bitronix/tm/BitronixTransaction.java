package bitronix.tm;

import bitronix.tm.internal.*;
import bitronix.tm.journal.Journal;
import bitronix.tm.twopc.*;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAResourceHolder;
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
    private List synchronizations = Collections.synchronizedList(new ArrayList());
    private boolean timeout = false;

    private static Preparer preparer = new Preparer(TransactionManagerServices.getExecutor());
    private static Committer committer = new Committer(TransactionManagerServices.getExecutor());
    private static Rollbacker rollbacker = new Rollbacker(TransactionManagerServices.getExecutor());

    /* management */
    private String threadName;
    private Date startDate;


    public BitronixTransaction() {
        Uid gtrid = UidGenerator.generateUid();
        if (log.isDebugEnabled()) log.debug("creating new transaction with GTRID [" + gtrid + "]");
        this.resourceManager = new XAResourceManager(gtrid);

        this.threadName = Thread.currentThread().getName();
        this.startDate = new Date();
    }

    public int getStatus() throws SystemException {
        return status;
    }

    public boolean enlistResource(XAResource xaResource) throws RollbackException, IllegalStateException, SystemException {
        if (status == Status.STATUS_MARKED_ROLLBACK)
            throw new BitronixRollbackException("transaction has been marked as rollback only");
        if (isDone())
            throw new IllegalStateException("transaction started or finished 2PC, cannot enlist any more resource");

        XAResourceHolder XAResourceHolder = ResourceRegistrar.findXAResourceHolder(xaResource);
        if (XAResourceHolder == null)
            throw new BitronixSystemException("unknown XAResource " + xaResource + ", it does not belong to a registered resource");
        XAResourceHolderState holder = XAResourceHolder.getXAResourceHolderState();

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
        synchronizations.add(synchronization);
    }

    public List getSynchronizations() {
        return synchronizations;
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("transaction hasn't started yet");
        if (isDone())
            throw new IllegalStateException("transaction is done, cannot commit it");

        fireBeforeCompletionEvent();
        if (status == Status.STATUS_MARKED_ROLLBACK) {
            if (log.isDebugEnabled()) log.debug("beforeCompletion synchronization(s) marked transaction as rollback only");
            rollback();
            throw new BitronixRollbackException("transaction was marked as rollback only and has been rolled back");
        }

        resourceManager.delistUnclosedResources(this);

        Map interestedResources = null;
        try {
            if (log.isDebugEnabled()) log.debug("committing, " + resourceManager.size() + " enlisted resource(s)");

            interestedResources = preparer.prepare(this);

            if (log.isDebugEnabled()) log.debug(interestedResources.size() + " interested resource(s)");

            committer.commit(this, interestedResources);

            if (log.isDebugEnabled()) log.debug("successfully committed " + toString());
        }
        catch (RuntimeException ex) {
            if (log.isDebugEnabled()) log.debug("caught runtime exception during commit, trying to rollback before throwing RollbackException", ex);
            try { lastChanceRollback(); } catch (BitronixSystemException ex2) { if (log.isDebugEnabled()) log.debug("last chance rollback failed", ex2);}
            throw new BitronixRollbackException("caught runtime exception during commit", ex);
        }
        catch (RollbackException ex) {
            if (log.isDebugEnabled()) log.debug("caught rollback exception during prepare, trying to rollback before rethrowing it", ex);
            try { lastChanceRollback(); } catch (BitronixSystemException ex2) { if (log.isDebugEnabled()) log.debug("last chance rollback failed", ex2);}
            throw ex;
        }
        catch (TransactionTimeoutException ex) {
            if (interestedResources == null) {
                if (log.isDebugEnabled()) log.debug("transaction timed out during prepare, trying to rollback before handling error");
                try { lastChanceRollback(); } catch (BitronixSystemException ex2) { if (log.isDebugEnabled()) log.debug("last chance rollback failed", ex2);}
                throw new BitronixRollbackException("transaction timed out during prepare", ex);
            }
            else {
                if (log.isDebugEnabled()) log.debug("transaction timed out during commit, prepared resources: " + resourceManager.size());
                throw new BitronixSystemException("transaction timed out due to unrecoverable resource error during commit. Transaction has been left in-doubt !", ex);
            }
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

        resourceManager.delistUnclosedResources(this);

        try {
            if (log.isDebugEnabled()) log.debug("rolling back, " + resourceManager.size() + " enlisted resource(s)");

            rollbacker.rollback(this);

            if (log.isDebugEnabled()) log.debug("successfully rolled back " + toString());
        }
        catch (RuntimeException ex) {
            if (log.isDebugEnabled()) log.debug("caught runtime exception during rollback, trying to rollback again before throwing SystemException", ex);
            try { lastChanceRollback(); } catch (BitronixSystemException ex2) { if (log.isDebugEnabled()) log.debug("last chance rollback failed", ex2);}
            throw new BitronixSystemException("caught runtime exception during rollback", ex);
        }
        catch (TransactionTimeoutException ex) {
            if (log.isDebugEnabled()) log.debug("transaction timed out during rollback phase");
            throw new BitronixSystemException("transaction timed out due to unrecoverable resource error during rollback phase. Transaction has been left in-doubt !", ex);
        } catch (BitronixHeuristicMixedException ex) {
            throw new BitronixSystemException("transaction partly committed and partly rolled back. Resources are now inconsistent !", ex);
        } catch (BitronixHeuristicCommitException ex) {
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

    public void setTimeout(Date date) throws IllegalStateException {
         if (isDone())
            throw new IllegalStateException("transaction is done, cannot set a timeout anymore");
        TransactionManagerServices.getTaskScheduler().scheduleTransactionTimeout(this, date);
    }

    public void timeout() {
        this.timeout = true;
    }

    public boolean timedOut() {
        return timeout;
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

    private void lastChanceRollback() throws BitronixSystemException {
        try {
            rollbacker.rollback(this);
            if (log.isDebugEnabled()) log.debug("last chance rollback succeeded");
        } catch (TransactionTimeoutException ex) {
            log.error("rollback failed due to unrecoverable resource error after time out. Transaction has been left in-doubt !", ex);
        } catch (BitronixHeuristicMixedException ex) {
            throw new BitronixSystemException("transaction partly committed and partly rolled back. Global transaction state is now inconsistent !", ex);
        } catch (BitronixHeuristicCommitException ex) {
            throw new BitronixSystemException("transaction committed instead of rolled back. Global transaction state is now inconsistent !", ex);
        }
    }

    /**
     * Run all registered Synchronizations' beforeCompletion() method. Be aware that this method can change the
     * transaction status to mark it as rollback only for instance.
     * @throws bitronix.tm.internal.BitronixSystemException
     */
    private void fireBeforeCompletionEvent() throws BitronixSystemException {
        if (log.isDebugEnabled()) log.debug("before completion, " + synchronizations.size() + " synchronization(s) to execute");
        for (int i = 0; i < synchronizations.size(); i++) {
            Synchronization synchronization = (Synchronization) synchronizations.get(i);
            try {
                if (log.isDebugEnabled()) log.debug("executing synchronization " + synchronization);
                synchronization.beforeCompletion();
            } catch (RuntimeException ex) {
                log.warn("Synchronization.beforeCompletion() call failed for " + synchronization + ", marking transaction as rollback only", ex);
                setStatus(Status.STATUS_MARKED_ROLLBACK);
                return;
            }
        }

        ManagementRegistrar.unregister("bitronix.tm:type=Transaction,Gtrid=" + resourceManager.getGtrid());
    }

    private void fireAfterCompletionEvent() {
        if (log.isDebugEnabled()) log.debug("after completion, " + synchronizations.size() + " synchronization(s) to execute");
        for (int i = 0; i < synchronizations.size(); i++) {
            Synchronization synchronization = (Synchronization) synchronizations.get(i);
            try {
                if (log.isDebugEnabled()) log.debug("executing synchronization " + synchronization + " with status=" + Decoder.decodeStatus(status));
                synchronization.afterCompletion(status);
            } catch (Exception ex) {
                log.warn("Synchronization.afterCompletion() call failed for " + synchronization, ex);
            }
        } // for
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
