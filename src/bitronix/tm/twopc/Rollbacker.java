package bitronix.tm.twopc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bitronix.tm.BitronixTransaction;
import bitronix.tm.utils.Decoder;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.twopc.executor.Job;
import bitronix.tm.internal.*;

import javax.transaction.Status;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicCommitException;
import javax.transaction.xa.XAException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;

/**
 * Phase 1 & 2 Rollback logic engine.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class Rollbacker extends AbstractPhaseEngine {

    private final static Logger log = LoggerFactory.getLogger(Rollbacker.class);

    private List interestedResources;
    // this list has to be thread-safe as the RollbackJobs can be executed in parallel (when async 2PC is configured)
    private final List rolledbackResources = Collections.synchronizedList(new ArrayList());

    public Rollbacker(Executor executor) {
        super(executor);
    }

    /**
     * Rollback the current XA transaction. {@link bitronix.tm.internal.TransactionTimeoutException} won't be thrown
     * while changing status but rather by some extra logic that will manually throw the exception after doing as much
     * cleanup as possible.
     *
     * @param transaction the transaction to rollback.
     * @param interestedResources resources that should be rolled back.
     * @throws HeuristicCommitException when all resources committed instead.
     * @throws HeuristicMixedException when some resources committed and some rolled back.
     * @throws bitronix.tm.internal.BitronixSystemException when an internal error occured.
     */
    public void rollback(BitronixTransaction transaction, List interestedResources) throws HeuristicMixedException, HeuristicCommitException, BitronixSystemException {
        XAResourceManager resourceManager = transaction.getResourceManager();
        transaction.setStatus(Status.STATUS_ROLLING_BACK);
        this.interestedResources = Collections.unmodifiableList(interestedResources);

        try {
            executePhase(resourceManager, true);
        } catch (PhaseException ex) {
            logFailedResources(ex);
            transaction.setStatus(Status.STATUS_UNKNOWN);
            throwException("transaction failed during rollback of " + transaction, ex, interestedResources.size());
        }

        if (log.isDebugEnabled()) log.debug("rollback executed on resources " + Decoder.collectResourcesNames(rolledbackResources));

        // Some resources might have failed the 2nd phase of 2PC.
        // Only resources which successfully rolled back should be registered in the journal, the other
        // ones should be picked up by the recoverer.
        // Not interested resources have to be included as well since they returned XA_RDONLY and they
        // don't participate in phase 2: the TX succeded for them.
        List rolledbackAndNotInterestedUniqueNames = new ArrayList();
        rolledbackAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(rolledbackResources));
        List notInterestedResources = collectNotInterestedResources(resourceManager.getAllResources(), interestedResources);
        rolledbackAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(notInterestedResources));

        if (log.isDebugEnabled()) {
            List rolledbackAndNotInterestedResources = new ArrayList();
            rolledbackAndNotInterestedResources.addAll(rolledbackResources);
            rolledbackAndNotInterestedResources.addAll(notInterestedResources);

            log.debug("rollback succeeded on resources " + Decoder.collectResourcesNames(rolledbackAndNotInterestedResources));
        }

        transaction.setStatus(Status.STATUS_ROLLEDBACK, new HashSet(rolledbackAndNotInterestedUniqueNames));
    }

    private void throwException(String message, PhaseException phaseException, int totalResourceCount) throws HeuristicMixedException, HeuristicCommitException {
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
            throw new BitronixHeuristicCommitException(message + ":" +
                    " all resource(s) " + Decoder.collectResourcesNames(heuristicResources) +
                    " improperly unilaterally committed", phaseException);
        else
            throw new BitronixHeuristicMixedException(message + ":" +
                    (errorResources.size() > 0 ? " resource(s) " + Decoder.collectResourcesNames(errorResources) + " threw unexpected exception" : "") +
                    (errorResources.size() > 0 && heuristicResources.size() > 0 ? " and" : "") +
                    (heuristicResources.size() > 0 ? " resource(s) " + Decoder.collectResourcesNames(heuristicResources) + " improperly unilaterally committed" + (hazard ? " (or hazard happened)" : "") : ""), phaseException);
    }

    protected Job createJob(XAResourceHolderState resourceHolder) {
        return new RollbackJob(resourceHolder);
    }

    protected boolean isParticipating(XAResourceHolderState xaResourceHolderState) {
        for (int i = 0; i < interestedResources.size(); i++) {
            XAResourceHolderState resourceHolderState = (XAResourceHolderState) interestedResources.get(i);
            if (xaResourceHolderState == resourceHolderState)
                return true;
        }
        return false;
    }

    private class RollbackJob extends Job {

        public RollbackJob(XAResourceHolderState resourceHolder) {
            super(resourceHolder);
        }

        public void run() {
            try {
                rollbackResource(getResource());
            } catch (RuntimeException ex) {
                runtimeException = ex;
            } catch (XAException ex) {
                xaException = ex;
            }
        }

        private void rollbackResource(XAResourceHolderState resourceHolder) throws XAException {
            try {
                if (log.isDebugEnabled()) log.debug("trying to rollback resource " + resourceHolder);
                resourceHolder.getXAResource().rollback(resourceHolder.getXid());
                rolledbackResources.add(resourceHolder);
                if (log.isDebugEnabled()) log.debug("rolled back resource " + resourceHolder);
            } catch (XAException ex) {
                handleXAException(resourceHolder, ex);
            }
        }

        private void handleXAException(XAResourceHolderState failedResourceHolder, XAException xaException) throws XAException {
            switch (xaException.errorCode) {
                case XAException.XA_HEURRB:
                    forgetHeuristicRollback(failedResourceHolder);
                    return;

                case XAException.XA_HEURCOM:
                case XAException.XA_HEURHAZ:
                case XAException.XA_HEURMIX:
                    log.error("heuristic rollback is incompatible with the global state of this transaction - guilty: " + failedResourceHolder);
                    throw xaException;

                default:
                    log.warn("resource '" + failedResourceHolder.getUniqueName() + "' reported " + Decoder.decodeXAExceptionErrorCode(xaException) +
                            " when asked to rollback transaction branch. Transaction is prepared and will rollback via recovery service when resource availability allows.", xaException);
            }
        }

        private void forgetHeuristicRollback(XAResourceHolderState resourceHolder) {
            try {
                if (log.isDebugEnabled()) log.debug("handling heuristic rollback on resource " + resourceHolder.getXAResource());
                resourceHolder.getXAResource().forget(resourceHolder.getXid());
                if (log.isDebugEnabled()) log.debug("forgotten heuristically rolled back resource " + resourceHolder.getXAResource());
            } catch (XAException ex) {
                log.error("cannot forget " + resourceHolder.getXid() + " assigned to " + resourceHolder.getXAResource() + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
            }
        }

        public String toString() {
            return "a RollbackJob with " + getResource();
        }
    }

}
