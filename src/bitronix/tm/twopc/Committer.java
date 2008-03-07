package bitronix.tm.twopc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Decoder;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.twopc.executor.Job;
import bitronix.tm.internal.*;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Phase 2 Commit logic holder.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class Committer {

    private final static Logger log = LoggerFactory.getLogger(Committer.class);

    private Executor executor;


    public Committer(Executor executor) {
        this.executor = executor;
    }

    /**
     * Execute phase 2 commit.
     * @param transaction the transaction wanting to commit phase 2
     * @param interestedResources a map of phase 1 prepared resources wanting to participate in phase 2 using Xids as keys
     * @throws bitronix.tm.internal.TransactionTimeoutException
     * @throws javax.transaction.HeuristicMixedException
     * @throws javax.transaction.HeuristicRollbackException
     * @throws bitronix.tm.internal.BitronixSystemException
     */
    public void commit(BitronixTransaction transaction, Map interestedResources) throws TransactionTimeoutException, HeuristicMixedException, HeuristicRollbackException, BitronixSystemException {
        XAResourceManager resourceManager = transaction.getResourceManager();
        if (resourceManager.size() == 0) {
            transaction.setStatus(Status.STATUS_COMMITTING);
            transaction.setStatus(Status.STATUS_COMMITTED);
            if (log.isDebugEnabled()) log.debug("phase 2 commit succeeded with no interested resource");
            return;
        }

        transaction.setStatus(Status.STATUS_COMMITTING);

        List jobs = new ArrayList();

        // start committing jobs
        if (log.isDebugEnabled()) log.debug("executing commit phase, " + interestedResources.size() + " interested resource(s) left");
        Iterator it = interestedResources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            XAResourceHolderState resourceHolder = (XAResourceHolderState) entry.getValue();

            // 1PC is when there is only one enlisted resource, not when there is only one participating one
            boolean onePhase = resourceManager.size() == 1;
            
            CommitJob job = new CommitJob(transaction, resourceHolder, onePhase);
            Object future = executor.submit(job);
            job.setFuture(future);
            jobs.add(job);
        }

        // wait for committing jobs to finish
        for (int i = 0; i < jobs.size(); ) {
            CommitJob job = (CommitJob) jobs.get(i);
            while (!executor.isDone(job.getFuture())) {
                if (transaction.timedOut())
                    throw new TransactionTimeoutException("transaction timed out during commit on " + job.getResource() + " (committed " + i + " out of " + jobs.size() + ")");
                executor.waitFor(job.getFuture(), 1000);
            }
            i++;
        }

        // check committing jobs return code
        List heuristicExceptions = new ArrayList();
        boolean hazard = false;
        for (int i = 0; i < jobs.size(); i++) {
            CommitJob job = (CommitJob) jobs.get(i);
            XAException xaException = job.getXAException();
            RuntimeException runtimeException = job.getRuntimeException();
            TransactionTimeoutException transactionTimeoutException = job.getTransactionTimeoutException();

            if (xaException != null) {
                if (xaException.errorCode == XAException.XA_HEURHAZ)
                    hazard = true;
                heuristicExceptions.add(xaException);
                log.error("cannot commit resource " + job.getResource() + ", error=" + Decoder.decodeXAExceptionErrorCode(xaException) + ", will not retry", xaException);
            }
            else if (runtimeException != null) {
                throw runtimeException;
            }
            else if (transactionTimeoutException != null) {
                throw transactionTimeoutException;
            }
        }

        // status can be marked as COMMITTED anyway for journal consistency, heuristic errors cannot be fixed and should only be reported
        transaction.setStatus(Status.STATUS_COMMITTED);

        if (heuristicExceptions.size() > 0) {
            if (!hazard && heuristicExceptions.size() == resourceManager.size())
                throw new BitronixHeuristicRollbackException("all " + resourceManager.size() + " resources improperly heuristically rolled back");
            else
                throw new BitronixHeuristicMixedException(heuristicExceptions.size() + " resource(s) out of " + resourceManager.size() + " improperly heuristically rolled back" + (hazard ? " (or maybe not)" : ""));
        }
    }

    /**
     * commit the resource, retrying as many times as necessary
     * @param transaction
     * @param resourceHolder
     * @param onePhase
     * @throws javax.transaction.xa.XAException if it is useless to retry, ie: heuristic happened
     * @throws bitronix.tm.internal.TransactionTimeoutException
     */
    private static void commitResource(BitronixTransaction transaction, XAResourceHolderState resourceHolder, boolean onePhase) throws XAException, TransactionTimeoutException {
        if (log.isDebugEnabled()) log.debug("committing resource " + resourceHolder + (onePhase ? " (with one-phase optimization)" : ""));
        while (true) {
            try {
                resourceHolder.getXAResource().commit(resourceHolder.getXid(), onePhase);
            } catch (XAException ex) {
                boolean fixed = handleXAException(resourceHolder, ex);
                if (!fixed) {
                    if (transaction.timedOut())
                        throw new TransactionTimeoutException("time out during phase 2 commit of " + transaction, ex);
                    int transactionRetryInterval = TransactionManagerServices.getConfiguration().getTransactionRetryInterval();
                    log.error("cannot commit phase 2 resource " + resourceHolder + ", error=" + Decoder.decodeXAExceptionErrorCode(ex) + ", retrying in " + transactionRetryInterval + "s", ex);
                    try {
                        Thread.sleep(transactionRetryInterval * 1000L);
                    } catch (InterruptedException iex) {
                        // ignored
                    }
                    continue;
                } // if
            }
            break;
        } // while
        if (log.isDebugEnabled()) log.debug("committed resource " + resourceHolder);
    }

    /**
     * @throws javax.transaction.xa.XAException is the commit should not be retried, ie: non-commit heuristic happened
     * @return true if the exception was about heuristic commit and was properly forgotten
     * @param failedResourceHolder
     * @param xaException
     */
    private static boolean handleXAException(XAResourceHolderState failedResourceHolder, XAException xaException) throws XAException {
        switch (xaException.errorCode) {
            case XAException.XA_HEURCOM:
                forgetHeuristicCommit(failedResourceHolder);
                // this exception has been fixed
                return true;

            case XAException.XAER_NOTA:
                throw new BitronixXAException("resource reported XAER_NOTA when asked to commit transaction branch", XAException.XA_HEURHAZ, xaException);

            case XAException.XA_HEURHAZ:
            case XAException.XA_HEURMIX:
            case XAException.XA_HEURRB:
                // this exception has not been fixed, DO NOT try again
                log.error("heuristic commit is incompatible with the global state of this transaction - guilty: " + failedResourceHolder);
                throw xaException;

            default:
                // this exception has not been fixed, try again later
                return false;
        }
    }

    private static void forgetHeuristicCommit(XAResourceHolderState resourceHolder) {
        if (log.isDebugEnabled()) log.debug("handling heuristic commit on resource " + resourceHolder.getXAResource());
        try {
            resourceHolder.getXAResource().forget(resourceHolder.getXid());
            if (log.isDebugEnabled()) log.debug("forgotten heuristically committed resource " + resourceHolder.getXAResource());
        } catch (XAException ex) {
            log.error("cannot forget " + resourceHolder.getXid() + " assigned to " + resourceHolder.getXAResource() + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
        }
    }

    private static class CommitJob extends Job {
        private BitronixTransaction transaction;
        private boolean onePhase;
        private TransactionTimeoutException transactionTimeoutException;

        public CommitJob(BitronixTransaction transaction, XAResourceHolderState resourceHolder, boolean onePhase) {
            super(resourceHolder);
            this.transaction = transaction;
            this.onePhase = onePhase;
        }

        public XAException getXAException() {
            return xaException;
        }

        public RuntimeException getRuntimeException() {
            return runtimeException;
        }

        public TransactionTimeoutException getTransactionTimeoutException() {
            return transactionTimeoutException;
        }

        public void run() {
            try {
                commitResource(transaction, getResource(), onePhase);
            } catch (RuntimeException ex) {
                runtimeException = ex;
            } catch (XAException ex) {
                xaException = ex;
            } catch (TransactionTimeoutException ex) {
                transactionTimeoutException = ex;
            }
        }
    }

}
