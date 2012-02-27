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
package bitronix.tm.twopc;

import bitronix.tm.TransactionManagerServices;
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
import java.util.Set;

/**
 * Phase 2 Commit logic engine.
 *
 * @author lorban
 */
public final class Committer extends AbstractPhaseEngine {

    private final static Logger log = LoggerFactory.getLogger(Committer.class);

    private volatile boolean onePhase;
    private final List<XAResourceHolderState> interestedResources = Collections.synchronizedList(new ArrayList<XAResourceHolderState>());
    // this list has to be thread-safe as the CommitJobs can be executed in parallel (when async 2PC is configured)
    private final List<XAResourceHolderState> committedResources = Collections.synchronizedList(new ArrayList<XAResourceHolderState>());


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
     * @throws bitronix.tm.internal.BitronixRollbackException during 1PC when resource fails to commit
     */
    public void commit(BitronixTransaction transaction, List<XAResourceHolderState> interestedResources) throws HeuristicMixedException, HeuristicRollbackException, BitronixSystemException, BitronixRollbackException {
        XAResourceManager resourceManager = transaction.getResourceManager();
        if (resourceManager.size() == 0) {
            transaction.setStatus(Status.STATUS_COMMITTING); //TODO: there is a disk force here that could be avoided
            transaction.setStatus(Status.STATUS_COMMITTED);
            if (log.isDebugEnabled()) log.debug("phase 2 commit succeeded with no interested resource");
            return;
        }

        transaction.setStatus(Status.STATUS_COMMITTING);

        this.interestedResources.clear();
        this.interestedResources.addAll(interestedResources);
        this.onePhase = resourceManager.size() == 1;

        try {
            executePhase(resourceManager, true);
        } catch (PhaseException ex) {
            logFailedResources(ex);
            if (onePhase) {
                transaction.setStatus(Status.STATUS_ROLLEDBACK);
                throw new BitronixRollbackException("transaction failed during 1PC commit of " + transaction, ex);
            } else {
                transaction.setStatus(Status.STATUS_UNKNOWN);
                throwException("transaction failed during commit of " + transaction, ex, interestedResources.size());
            }
        }

        if (log.isDebugEnabled()) log.debug("phase 2 commit executed on resources " + Decoder.collectResourcesNames(committedResources));

        // Some resources might have failed the 2nd phase of 2PC.
        // Only resources which successfully committed should be registered in the journal, the other
        // ones should be picked up by the recoverer.
        // Not interested resources have to be included as well since they returned XA_RDONLY and they
        // don't participate in phase 2: the TX succeded for them.
        Set<String> committedAndNotInterestedUniqueNames = new HashSet<String>();
        committedAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(committedResources));
        List<XAResourceHolderState> notInterestedResources = collectNotInterestedResources(resourceManager.getAllResources(), interestedResources);
        committedAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(notInterestedResources));

        if (log.isDebugEnabled()) {
            List<XAResourceHolderState> committedAndNotInterestedResources = new ArrayList<XAResourceHolderState>();
            committedAndNotInterestedResources.addAll(committedResources);
            committedAndNotInterestedResources.addAll(notInterestedResources);

            log.debug("phase 2 commit succeeded on resources " + Decoder.collectResourcesNames(committedAndNotInterestedResources));
        }

        transaction.setStatus(Status.STATUS_COMMITTED, committedAndNotInterestedUniqueNames);
    }

    private void throwException(String message, PhaseException phaseException, int totalResourceCount) throws HeuristicMixedException, HeuristicRollbackException {
        List<Exception> exceptions = phaseException.getExceptions();
        List<XAResourceHolderState> resources = phaseException.getResourceStates();

        boolean hazard = false;
        List<XAResourceHolderState> heuristicResources = new ArrayList<XAResourceHolderState>();
        List<XAResourceHolderState> errorResources = new ArrayList<XAResourceHolderState>();

        for (int i = 0; i < exceptions.size(); i++) {
            Exception ex = exceptions.get(i);
            XAResourceHolderState resourceHolder = resources.get(i);
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
        for (XAResourceHolderState resourceHolderState : interestedResources) {
            if (xaResourceHolderState == resourceHolderState)
                return true;
        }
        return false;
    }


    private final class CommitJob extends Job {

        public CommitJob(XAResourceHolderState resourceHolder) {
            super(resourceHolder);
        }

        public XAException getXAException() {
            return xaException;
        }

        public RuntimeException getRuntimeException() {
            return runtimeException;
        }

        public void execute() {
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
               handleXAException(resourceHolder, ex, onePhase);
            }
        }

        private void handleXAException(XAResourceHolderState failedResourceHolder, XAException xaException, boolean onePhase) throws XAException {
            switch (xaException.errorCode) {
                case XAException.XA_HEURCOM:
                    forgetHeuristicCommit(failedResourceHolder);
                    return;

                case XAException.XAER_NOTA:
                    throw new BitronixXAException("unknown heuristic termination, global state of this transaction is unknown - guilty: " + failedResourceHolder, XAException.XA_HEURHAZ, xaException);

                case XAException.XA_HEURHAZ:
                case XAException.XA_HEURMIX:
                case XAException.XA_HEURRB:
                case XAException.XA_RBCOMMFAIL:
                case XAException.XA_RBDEADLOCK:
                case XAException.XA_RBINTEGRITY:
                case XAException.XA_RBOTHER:
                case XAException.XA_RBPROTO:
                case XAException.XA_RBROLLBACK:
                case XAException.XA_RBTIMEOUT:
                case XAException.XA_RBTRANSIENT:
                    log.error("heuristic rollback is incompatible with the global state of this transaction - guilty: " + failedResourceHolder);
                    throw xaException;

                default:
                    if (onePhase) {
                        if (log.isDebugEnabled()) log.debug("XAException thrown in commit phase of 1PC optimization, rethrowing it");
                        throw xaException;
                    }
                    String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer().extractExtraXAExceptionDetails(xaException);
                    log.warn("resource '" + failedResourceHolder.getUniqueName() + "' reported " + Decoder.decodeXAExceptionErrorCode(xaException) +
                            (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails) + " when asked to commit transaction branch." +
                            " Transaction is prepared and will commit via recovery service when resource availability allows.", xaException);
            }
        }

        private void forgetHeuristicCommit(XAResourceHolderState resourceHolder) {
            try {
                if (log.isDebugEnabled()) log.debug("handling heuristic commit on resource " + resourceHolder.getXAResource());
                resourceHolder.getXAResource().forget(resourceHolder.getXid());
                if (log.isDebugEnabled()) log.debug("forgotten heuristically committed resource " + resourceHolder.getXAResource());
            } catch (XAException ex) {
                String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer().extractExtraXAExceptionDetails(ex);
                log.error("cannot forget " + resourceHolder.getXid() + " assigned to " + resourceHolder.getXAResource() +
                        ", error=" + Decoder.decodeXAExceptionErrorCode(ex) + (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
            }
        }

        public String toString() {
            return "a CommitJob " + (onePhase ? "(one phase) " : "") + "with " + getResource();
        }
    }

}
