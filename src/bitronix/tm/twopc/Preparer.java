package bitronix.tm.twopc;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Decoder;
import bitronix.tm.internal.*;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.twopc.executor.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Status;
import javax.transaction.RollbackException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.util.*;

/**
 * Phase 1 Prepare logic engine.
 * <p>&copy; Bitronix 2005, 2006, 2007, 2008</p>
 *
 * @author lorban
 */
public class Preparer extends AbstractPhaseEngine {

    private final static Logger log = LoggerFactory.getLogger(Preparer.class);

    private List preparedResources;

    public Preparer(Executor executor) {
        super(executor);
    }

    /**
     * Execute phase 1 prepare.
     * @param transaction the transaction to prepare.
     * @param interestedResources a list that will be filled with all resources that received the prepare command
     *  and replied with {@link javax.transaction.xa.XAResource#XA_OK}.
     * @throws RollbackException when an error occured that can be fixed with a rollback.
     * @throws bitronix.tm.internal.BitronixSystemException when an internal error occured.
     */
    public void prepare(BitronixTransaction transaction, List interestedResources) throws RollbackException, BitronixSystemException {
        XAResourceManager resourceManager = transaction.getResourceManager();
        transaction.setStatus(Status.STATUS_PREPARING);
        this.preparedResources = interestedResources;

        if (resourceManager.size() == 0) {
            if (TransactionManagerServices.getConfiguration().isWarnAboutZeroResourceTransaction())
                log.warn("executing transaction with 0 enlisted resource");
            else
                if (log.isDebugEnabled()) log.debug("0 resource enlisted, no prepare needed");

            transaction.setStatus(Status.STATUS_PREPARED);
            return;
        }

        // 1PC optimization
        if (resourceManager.size() == 1) {
            XAResourceHolderState resourceHolder = (XAResourceHolderState) resourceManager.getAllResources().get(0);

            preparedResources.add(resourceHolder);
            if (log.isDebugEnabled()) log.debug("1 resource enlisted, no prepare needed (1PC)");
            transaction.setStatus(Status.STATUS_PREPARED);
            return;
        }

        try {
            executePhase(resourceManager, false);
        } catch (PhaseException ex) {
            logFailedResources(ex);
            throwException("transaction failed during prepare of " + transaction, ex);
        }

        transaction.setStatus(Status.STATUS_PREPARED);
        if (log.isDebugEnabled()) log.debug("successfully prepared " + preparedResources.size() + " resource(s)");
    }

    private void throwException(String message, PhaseException phaseException) throws BitronixRollbackException {
        List exceptions = phaseException.getExceptions();
        List resources = phaseException.getResources();

        List heuristicResources = new ArrayList();
        List errorResources = new ArrayList();

        for (int i = 0; i < exceptions.size(); i++) {
            Exception ex = (Exception) exceptions.get(i);
            XAResourceHolderState resourceHolder = (XAResourceHolderState) resources.get(i);
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
                    " resource(s) " + collectResourcesNames(heuristicResources) +
                    " unilaterally finished transaction branch before being asked to prepare", phaseException);
        else
            throw new BitronixRollbackException(message + ":" +
                    " resource(s) " + collectResourcesNames(errorResources) +
                    " threw unexpected exception", phaseException);
    }

    protected Job createJob(XAResourceHolderState xaResourceHolderState) {
        return new PrepareJob(xaResourceHolderState, preparedResources);
    }

    protected boolean isParticipating(XAResourceHolderState xaResourceHolderState) {
        return true;
    }


    private static class PrepareJob extends Job {
        private final List preparedResources;

        public PrepareJob(XAResourceHolderState resourceHolder, List preparedResources) {
            super(resourceHolder);
            this.preparedResources = preparedResources;
        }

        public void run() {
            try {
                XAResourceHolderState resourceHolder = getResource();
                if (log.isDebugEnabled()) log.debug("preparing resource " + resourceHolder);

                int vote = resourceHolder.getXAResource().prepare(resourceHolder.getXid());
                if (vote != XAResource.XA_RDONLY) {
                    synchronized (preparedResources) {
                        preparedResources.add(resourceHolder);
                    }
                }

                if (log.isDebugEnabled()) log.debug("prepared resource " + resourceHolder + " voted " + Decoder.decodePrepareVote(vote));
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
