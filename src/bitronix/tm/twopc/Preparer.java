package bitronix.tm.twopc;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Decoder;
import bitronix.tm.internal.*;
import bitronix.tm.twopc.executor.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.HeuristicMixedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.*;

/**
 * Phase 1 Prepare logic holder.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class Preparer {

    private final static Logger log = LoggerFactory.getLogger(Preparer.class);

    private Executor executor;


    public Preparer(Executor executor) {
        this.executor = executor;
    }

    /**
     * Execute phase 1 prepare.
     * @return a map of resources that vote XA_OK using XIDs as keys.
     * @param transaction the transaction to prepare.
     * @throws bitronix.tm.internal.BitronixSystemException
     * @throws bitronix.tm.internal.TransactionTimeoutException
     * @throws javax.transaction.RollbackException
     */
    public Map prepare(BitronixTransaction transaction) throws TransactionTimeoutException, RollbackException, BitronixSystemException, HeuristicMixedException {
        if (transaction.timedOut())
            throw new TransactionTimeoutException("transaction timed out before 2PC execution");
        XAResourceManager resourceManager = transaction.getResourceManager();
        Map preparedResources = new HashMap();
        transaction.setStatus(Status.STATUS_PREPARING);

        if (resourceManager.size() == 0) {
            if (TransactionManagerServices.getConfiguration().isWarnAboutZeroResourceTransaction())
                log.warn("executing transaction with 0 enlisted resource");
            else
                if (log.isDebugEnabled()) log.debug("0 resource enlisted, no prepare needed");

            transaction.setStatus(Status.STATUS_PREPARED);
            return preparedResources;
        }

        // 1PC optimization
        if (resourceManager.size() == 1) {
            Map.Entry entry = (Map.Entry) resourceManager.entriesIterator().next();
            Xid xid = (Xid) entry.getKey();
            XAResourceHolderState resourceHolder = (XAResourceHolderState) entry.getValue();

            preparedResources.put(xid, resourceHolder);
            if (log.isDebugEnabled()) log.debug("1 resource enlisted, no prepare needed (1PC)");
            transaction.setStatus(Status.STATUS_PREPARED);
            return preparedResources;
        }

        List jobs = new ArrayList();

        // start preparing threads
        if (log.isDebugEnabled()) log.debug(resourceManager.size() + " resource(s) enlisted, preparing");
        XAResourceHolderState emulatingHolder = null;
        Iterator it = resourceManager.entriesIterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Xid xid = (Xid) entry.getKey();
            XAResourceHolderState resourceHolder = (XAResourceHolderState) entry.getValue();

            if (resourceHolder.getXAResourceHolder().isEmulatingXA()) {
                if (log.isDebugEnabled()) log.debug("keeping emulating resource for later: " + resourceHolder);
                emulatingHolder = resourceHolder;
            }
            else {
                PrepareJob job = new PrepareJob(resourceHolder, xid, preparedResources);
                Object future = executor.submit(job);
                job.setFuture(future);
                jobs.add(job);
            }
        }

        // wait for preparing threads to finish
        for (int i=0; i < jobs.size(); ) {
            PrepareJob job = (PrepareJob) jobs.get(i);
            Object future = job.getFuture();
            while (!executor.isDone(future)) {
                executor.waitFor(future, 1000);
                if (transaction.timedOut())
                    throw new TransactionTimeoutException("transaction timed out during prepare on " + job.getResource() + "(prepared " + i + " out of " + jobs.size() + ")");
            }
            i++;
        }

        // check preparing threads return code
        for (int i = 0; i < jobs.size(); i++) {
            PrepareJob job = (PrepareJob) jobs.get(i);
            XAException xaException = job.getXAException();
            RuntimeException runtimeException = job.getRuntimeException();

            if (xaException != null) {
                if (log.isDebugEnabled()) log.debug("error preparing resource, failed resource=" + job.resourceHolder + ", prepared resources: " + resourceManager.size() + ", errorCode=" + Decoder.decodeXAExceptionErrorCode(xaException));
                throwException(job.resourceHolder, xaException);
            } else if (runtimeException != null) {
                throw runtimeException;
            }
        }

        // Last Resource Commit
        if (emulatingHolder != null) {
            try {
                if (log.isDebugEnabled()) log.debug("preparing emulating resource " + emulatingHolder);
                int vote = emulatingHolder.getXAResource().prepare(emulatingHolder.getXid());
                if (vote != XAResource.XA_RDONLY) {
                    preparedResources.put(emulatingHolder.getXid(), emulatingHolder);
                }
                if (log.isDebugEnabled()) log.debug("prepared emulating resource " + emulatingHolder + " voted " + Decoder.decodePrepareVote(vote));
            } catch (XAException ex) {
                if (log.isDebugEnabled()) log.debug("error preparing emulating resource, failed resource=" + emulatingHolder + ", prepared resources: " + resourceManager.size() + ", error=" + Decoder.decodeXAExceptionErrorCode(ex));
                throwException(emulatingHolder, ex);
            }
        }

        transaction.setStatus(Status.STATUS_PREPARED);
        if (log.isDebugEnabled()) log.debug("successfully prepared " + preparedResources.size() + " resource(s)");
        return preparedResources;
    }

    private static void throwException(XAResourceHolderState holder, XAException xaException) throws HeuristicMixedException, BitronixRollbackException {
        switch (xaException.errorCode) {
            case XAException.XAER_NOTA:
                throw new BitronixHeuristicMixedException("resource " + holder.getUniqueName() + " unilaterally finished transaction branch when asked to prepare, global state of this transaction is now unknown", xaException);

            default:
                throw new BitronixRollbackException("transaction failed during prepare, error=" + Decoder.decodeXAExceptionErrorCode(xaException), xaException);
        }
    }


    private static void runPrepare(XAResourceHolderState resourceHolder, Xid xid, Map preparedResources) throws XAException {
        if (log.isDebugEnabled()) log.debug("preparing resource " + resourceHolder);
        int vote = resourceHolder.getXAResource().prepare(xid);
        if (vote != XAResource.XA_RDONLY) {
            preparedResources.put(resourceHolder.getXid(), resourceHolder);
        }
        if (log.isDebugEnabled()) log.debug("prepared resource " + resourceHolder + " voted " + Decoder.decodePrepareVote(vote));
    }


    private static class PrepareJob implements Runnable {
        private XAResourceHolderState resourceHolder;
        private Xid xid;
        private Map preparedResources;
        private XAException xaException;
        private RuntimeException runtimeException;
        private Object future;

        public PrepareJob(XAResourceHolderState resourceHolder, Xid xid, Map preparedResources) {
            this.resourceHolder = resourceHolder;
            this.xid = xid;
            this.preparedResources = Collections.synchronizedMap(preparedResources);
        }

        public XAResourceHolderState getResource() {
            return resourceHolder;
        }

        public XAException getXAException() {
            return xaException;
        }

        public RuntimeException getRuntimeException() {
            return runtimeException;
        }

        public void run() {
            try {
                runPrepare(resourceHolder, xid, preparedResources);
            } catch (RuntimeException ex) {
                runtimeException = ex;
            } catch (XAException ex) {
                xaException = ex;
            }
        }

        public void setFuture(Object future) {
            this.future = future;
        }

        public Object getFuture() {
            return future;
        }
    } // class

}
