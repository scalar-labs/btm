/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.twopc;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixRollbackException;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.internal.XAResourceManager;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.twopc.executor.Job;
import bitronix.tm.utils.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phase 1 Prepare logic engine.
 *
 * @author lorban
 */
public final class Preparer extends AbstractPhaseEngine {

    private final static Logger log = LoggerFactory.getLogger(Preparer.class);

    // this list has to be thread-safe as the PrepareJobs can be executed in parallel (when async 2PC is configured)
    private final List<XAResourceHolderState> preparedResources = Collections.synchronizedList(new ArrayList<XAResourceHolderState>());

    public Preparer(Executor executor) {
        super(executor);
    }

    /**
     * Execute phase 1 prepare.
     * @param transaction the transaction to prepare.
     * @return a list that will be filled with all resources that received the prepare command
     *  and replied with {@link javax.transaction.xa.XAResource#XA_OK}.
     * @throws RollbackException when an error occured that can be fixed with a rollback.
     * @throws bitronix.tm.internal.BitronixSystemException when an internal error occured.
     */
    public List<XAResourceHolderState> prepare(BitronixTransaction transaction) throws RollbackException, BitronixSystemException {
        XAResourceManager resourceManager = transaction.getResourceManager();
        transaction.setStatus(Status.STATUS_PREPARING);
        preparedResources.clear();

        if (resourceManager.size() == 0) {
            if (TransactionManagerServices.getConfiguration().isWarnAboutZeroResourceTransaction())
                log.warn("executing transaction with 0 enlisted resource");
            else
                if (log.isDebugEnabled()) { log.debug("0 resource enlisted, no prepare needed"); }

            transaction.setStatus(Status.STATUS_PREPARED);
            return preparedResources;
        }

        // 1PC optimization
        if (resourceManager.size() == 1) {
            XAResourceHolderState resourceHolder = resourceManager.getAllResources().get(0);

            preparedResources.add(resourceHolder);
            if (log.isDebugEnabled()) { log.debug("1 resource enlisted, no prepare needed (1PC)"); }
            transaction.setStatus(Status.STATUS_PREPARED);
            return preparedResources;
        }

        try {
            executePhase(resourceManager, false);
        } catch (PhaseException ex) {
            logFailedResources(ex);
            throwException("transaction failed during prepare of " + transaction, ex);
        }

        transaction.setStatus(Status.STATUS_PREPARED);
        if (log.isDebugEnabled()) { log.debug("successfully prepared " + preparedResources.size() + " resource(s)"); }
        return Collections.unmodifiableList(preparedResources);
    }

    private void throwException(String message, PhaseException phaseException) throws BitronixRollbackException {
        List<Exception> exceptions = phaseException.getExceptions();
        List<XAResourceHolderState> resources = phaseException.getResourceStates();

        List<XAResourceHolderState> heuristicResources = new ArrayList<XAResourceHolderState>();
        List<XAResourceHolderState> errorResources = new ArrayList<XAResourceHolderState>();

        for (int i = 0; i < exceptions.size(); i++) {
            Exception ex = exceptions.get(i);
            XAResourceHolderState resourceHolder = resources.get(i);
            if (ex instanceof XAException) {
                XAException xaEx = (XAException) ex;
                /**
                 * Sybase ASE can sometimes forget a transaction before prepare. For instance, when executing
                 * a stored procedure that contains a rollback statement. In that case it throws XAException(XAER_NOTA)
                 * when asked to prepare.
                 */
                if (xaEx.errorCode == XAException.XAER_NOTA)
                    heuristicResources.add(resourceHolder);
                else
                    errorResources.add(resourceHolder);
            }
            else
                errorResources.add(resourceHolder);
        }

        if (heuristicResources.size() > 0)
            throw new BitronixRollbackException(message + ":" +
                    " resource(s) " + Decoder.collectResourcesNames(heuristicResources) +
                    " unilaterally finished transaction branch before being asked to prepare", phaseException);
        else
            throw new BitronixRollbackException(message + ":" +
                    " resource(s) " + Decoder.collectResourcesNames(errorResources) +
                    " threw unexpected exception", phaseException);
    }

    protected Job createJob(XAResourceHolderState xaResourceHolderState) {
        return new PrepareJob(xaResourceHolderState);
    }

    protected boolean isParticipating(XAResourceHolderState xaResourceHolderState) {
        return true;
    }


    private final class PrepareJob extends Job {
        public PrepareJob(XAResourceHolderState resourceHolder) {
            super(resourceHolder);
        }

        public void execute() {
            try {
                XAResourceHolderState resourceHolder = getResource();
                if (log.isDebugEnabled()) { log.debug("preparing resource " + resourceHolder); }

                int vote = resourceHolder.getXAResource().prepare(resourceHolder.getXid());
                if (vote != XAResource.XA_RDONLY) {
                    preparedResources.add(resourceHolder);
                }

                if (log.isDebugEnabled()) { log.debug("prepared resource " + resourceHolder + " voted " + Decoder.decodePrepareVote(vote)); }
            } catch (RuntimeException ex) {
                runtimeException = ex;
            } catch (XAException ex) {
                xaException = ex;
            }
        }

        public String toString() {
            return "a PrepareJob with " + getResource();
        }
    }

}
