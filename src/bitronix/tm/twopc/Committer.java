package bitronix.tm.twopc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bitronix.tm.BitronixTransaction;
import bitronix.tm.utils.Decoder;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.twopc.executor.Job;
import bitronix.tm.internal.*;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;

/**
 * Phase 2 Commit logic engine.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class Committer extends AbstractPhaseEngine {

    private final static Logger log = LoggerFactory.getLogger(Committer.class);

    private boolean onePhase;
    private List interestedResources;
    // this list has to be thread-safe as the CommitJobs can be executed in parallel (when async 2PC is configured)
    private final List committedResources = Collections.synchronizedList(new ArrayList());


    public Committer(Executor executor) {
       super(executor);
    }

    /**
     * Execute phase 2 commit.
     * @param transaction the transaction wanting to commit phase 2
     * @param interestedResources a map of phase 1 prepared resources wanting to participate in phase 2 using Xids as keys
     * @throws HeuristicRollbackException when all resources committed instead.
     * @throws HeuristicMixedException when some resources committed and some rolled back.
     * @throws bitronix.tm.internal.BitronixSystemException when an internal error occured.
     */
    public void commit(BitronixTransaction transaction, List interestedResources) throws HeuristicMixedException, HeuristicRollbackException, BitronixSystemException {
        XAResourceManager resourceManager = transaction.getResourceManager();
        if (resourceManager.size() == 0) {
            transaction.setStatus(Status.STATUS_COMMITTING); //TODO: there is a disk force here that could be avoided
            transaction.setStatus(Status.STATUS_COMMITTED);
            if (log.isDebugEnabled()) log.debug("phase 2 commit succeeded with no interested resource");
            return;
        }

        transaction.setStatus(Status.STATUS_COMMITTING);

        this.interestedResources = Collections.unmodifiableList(interestedResources);
        this.onePhase = resourceManager.size() == 1;

        try {
            executePhase(resourceManager, true);
        } catch (PhaseException ex) {
            logFailedResources(ex);
            transaction.setStatus(Status.STATUS_UNKNOWN);
            throwException("transaction failed during commit of " + transaction, ex, interestedResources.size());
        }

        if (log.isDebugEnabled()) log.debug("phase 2 commit executed on resources " + Decoder.collectResourcesNames(committedResources));

        // Some resources might have failed the 2nd phase of 2PC.
        // Only resources which successfully committed should be registered in the journal, the other
        // ones should be picked up by the recoverer.
        // Not interested resources have to be included as well since they returned XA_RDONLY and they
        // don't participate in phase 2: the TX succeded for them.
        List committedAndNotInterestedUniqueNames = new ArrayList();
        committedAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(committedResources));
        List notInterestedResources = collectNotInterestedResources(resourceManager.getAllResources(), interestedResources);
        committedAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(notInterestedResources));

        if (log.isDebugEnabled()) {
            List committedAndNotInterestedResources = new ArrayList();
            committedAndNotInterestedResources.addAll(committedResources);
            committedAndNotInterestedResources.addAll(notInterestedResources);

            log.debug("phase 2 commit succeeded on resources " + Decoder.collectResourcesNames(committedAndNotInterestedResources));
        }

        transaction.setStatus(Status.STATUS_COMMITTED, new HashSet(committedAndNotInterestedUniqueNames));
    }

    private void throwException(String message, PhaseException phaseException, int totalResourceCount) throws HeuristicMixedException, HeuristicRollbackException {
        List exceptions = phaseException.getExceptions();
        List resources = phaseException.getResources();

        boolean hazard = false;
        List heuristicResources = new ArrayList();
        List errorResources = new ArrayList();

        for (int i = 0; i < exceptions.size(); i++) {
            Exception ex = (Exception) exceptions.get(i);
            XAResourceHolderState resourceHolder = (XAResourceHolderState) resources.get(i);
            if (ex instanceof XAException) {
                XAException xaEx = (XAException) ex;
                switch (xaEx.errorCode) {
                    case XAException.XA_HEURHAZ:
                        hazard = true;
                    case XAException.XA_HEURCOM:
                    case XAException.XA_HEURRB:
                    case XAException.XA_HEURMIX:
                        heuristicResources.add(resourceHolder);
                        break;

                    default:
                        errorResources.add(resourceHolder);
                }
            }
            else
                errorResources.add(resourceHolder);
        }

        if (!hazard && heuristicResources.size() == totalResourceCount)
            throw new BitronixHeuristicRollbackException(message + ":" +
                    " all resource(s) " + Decoder.collectResourcesNames(heuristicResources) +
                    " improperly unilaterally rolled back", phaseException);
        else
            throw new BitronixHeuristicMixedException(message + ":" +
                    (errorResources.size() > 0 ? " resource(s) " + Decoder.collectResourcesNames(errorResources) + " threw unexpected exception" : "") +
                    (errorResources.size() > 0 && heuristicResources.size() > 0 ? " and" : "") +
                    (heuristicResources.size() > 0 ? " resource(s) " + Decoder.collectResourcesNames(heuristicResources) + " improperly unilaterally rolled back" + (hazard ? " (or hazard happened)" : "") : ""), phaseException);
    }

    protected Job createJob(XAResourceHolderState resourceHolder) {
        return new CommitJob(resourceHolder);
    }

    protected boolean isParticipating(XAResourceHolderState xaResourceHolderState) {
        for (int i = 0; i < interestedResources.size(); i++) {
            XAResourceHolderState resourceHolderState = (XAResourceHolderState) interestedResources.get(i);
            if (xaResourceHolderState == resourceHolderState)
                return true;
        }
        return false;
    }


    private class CommitJob extends Job {

        public CommitJob(XAResourceHolderState resourceHolder) {
            super(resourceHolder);
        }

        public XAException getXAException() {
            return xaException;
        }

        public RuntimeException getRuntimeException() {
            return runtimeException;
        }

        public void run() {
            try {
                commitResource(getResource(), onePhase);
            } catch (RuntimeException ex) {
                runtimeException = ex;
            } catch (XAException ex) {
                xaException = ex;
            }
        }

        private void commitResource(XAResourceHolderState resourceHolder, boolean onePhase) throws XAException {
            try {
                if (log.isDebugEnabled()) log.debug("committing resource " + resourceHolder + (onePhase ? " (with one-phase optimization)" : ""));
                resourceHolder.getXAResource().commit(resourceHolder.getXid(), onePhase);
                committedResources.add(resourceHolder);
                if (log.isDebugEnabled()) log.debug("committed resource " + resourceHolder);
            } catch (XAException ex) {
               handleXAException(resourceHolder, ex);
            }
        }

        private void handleXAException(XAResourceHolderState failedResourceHolder, XAException xaException) throws XAException {
            switch (xaException.errorCode) {
                case XAException.XA_HEURCOM:
                    forgetHeuristicCommit(failedResourceHolder);
                    return;

                case XAException.XA_HEURHAZ:
                case XAException.XA_HEURMIX:
                case XAException.XA_HEURRB:
                    log.error("heuristic rollback is incompatible with the global state of this transaction - guilty: " + failedResourceHolder);
                    throw xaException;

                default:
                    log.warn("resource '" + failedResourceHolder.getUniqueName() + "' reported " + Decoder.decodeXAExceptionErrorCode(xaException) +
                            " when asked to commit transaction branch. Transaction is prepared and will commit via recovery service when resource availability allows.", xaException);
            }
        }

        private void forgetHeuristicCommit(XAResourceHolderState resourceHolder) {
            try {
                if (log.isDebugEnabled()) log.debug("handling heuristic commit on resource " + resourceHolder.getXAResource());
                resourceHolder.getXAResource().forget(resourceHolder.getXid());
                if (log.isDebugEnabled()) log.debug("forgotten heuristically committed resource " + resourceHolder.getXAResource());
            } catch (XAException ex) {
                log.error("cannot forget " + resourceHolder.getXid() + " assigned to " + resourceHolder.getXAResource() + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
            }
        }

        public String toString() {
            return "a CommitJob " + (onePhase ? "(one phase) " : "") + "with " + getResource();
        }
    }

}
