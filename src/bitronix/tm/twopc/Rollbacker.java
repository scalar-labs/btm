package bitronix.tm.twopc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Decoder;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.internal.*;

import javax.transaction.Status;
import javax.transaction.xa.XAException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

/**
 * Phase 1 & 2 Rollback logic holder.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class Rollbacker {

    private final static Logger log = LoggerFactory.getLogger(Rollbacker.class);

    private Executor executor;


    public Rollbacker(Executor executor) {
        this.executor = executor;
    }

    /**
     * Rollback the current XA transaction. {@link bitronix.tm.internal.TransactionTimeoutException} won't be thrown
     * while changing status but rather by some extra logic that will manually throw the exception after doing as much
     * cleanup as possible.
     *
     * @param transaction the transaction to rollback.
     */
    public void rollback(BitronixTransaction transaction) throws TransactionTimeoutException, BitronixHeuristicMixedException, BitronixHeuristicCommitException, BitronixSystemException {
        XAResourceManager resourceManager = transaction.getResourceManager();
        transaction.setStatus(Status.STATUS_ROLLING_BACK);

        List jobs = new ArrayList();

        Iterator it = resourceManager.entriesIterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            XAResourceHolderState resourceHolder = (XAResourceHolderState) entry.getValue();

            RollbackJob job = new RollbackJob(transaction, resourceHolder);
            Object future = executor.submit(job);
            job.setFuture(future);
            jobs.add(job);
        }


        // wait for rollback jobs to finish
        for (int i=0; i < jobs.size(); ) {
            RollbackJob job = (RollbackJob) jobs.get(i);
            Object future = job.getFuture();
            while (!executor.isDone(future)) {
                executor.waitFor(future, 1000);
                // do not check for timeout during rollback
            }
            i++;
        }

        // check rollback jobs return code
        List exceptions = new ArrayList();
        boolean hazard = false;
        for (int i = 0; i < jobs.size(); i++) {
            RollbackJob job = (RollbackJob) jobs.get(i);
            XAException xaException = job.getXAException();
            RuntimeException runtimeException = job.getRuntimeException();
            TransactionTimeoutException transactionTimeoutException = job.getTransactionTimeoutException();

            if (xaException != null) {
                if (xaException.errorCode == XAException.XA_HEURHAZ)
                    hazard = true;
                exceptions.add(xaException);
                log.error("transaction failed during rollback, error=" + Decoder.decodeXAExceptionErrorCode(xaException), xaException);
            }
            if (runtimeException != null) {
                exceptions.add(xaException);
                log.error("transaction failed during rollback", runtimeException);
            }
            if (transactionTimeoutException != null) {
                exceptions.add(xaException);
                if (log.isDebugEnabled()) log.debug("ignored transaction timeout during rollback of " + job.getResource());
            }
        }

        if (exceptions.size() > 0) {
            if (!hazard && exceptions.size() == resourceManager.size()) {
                throw new BitronixHeuristicCommitException("all " + resourceManager.size() + " resources improperly heuristically committed");
            }
            else
                throw new BitronixHeuristicMixedException(exceptions.size() + " resource(s) out of " + resourceManager.size() + " improperly heuristically committed" + (hazard ? " (or maybe not)" : ""));
        }
        else
            transaction.setStatus(Status.STATUS_ROLLEDBACK);
    }

    private static void rollbackResource(BitronixTransaction transaction, XAResourceHolderState resourceHolder) throws XAException, TransactionTimeoutException {
        while (true) {
            try {
                if (log.isDebugEnabled()) log.debug("trying to rollback resource " + resourceHolder);
                resourceHolder.getXAResource().rollback(resourceHolder.getXid());
                if (log.isDebugEnabled()) log.debug("rolled back resource " + resourceHolder);
            } catch (XAException ex) {
                boolean fixed = handleXAException(resourceHolder, ex);
                if (!fixed) {
                    if (transaction.timedOut())
                        throw new TransactionTimeoutException("time out during rollback of " + transaction, ex);

                    int transactionRetryInterval = TransactionManagerServices.getConfiguration().getTransactionRetryInterval();
                    log.error("cannot rollback resource " + resourceHolder + ", error=" + Decoder.decodeXAExceptionErrorCode(ex) + ", retrying in " + transactionRetryInterval + "s", ex);
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

    }

    /**
     * @param failedResourceHolder
     * @param xaException
     * @return true if the exception was about heuristic rollback and was properly forgotten
     * @throws javax.transaction.xa.XAException is the rollback should not be retried, ie: non-rollback heuristic happened
     */
    private static boolean handleXAException(XAResourceHolderState failedResourceHolder, XAException xaException) throws XAException {
          switch (xaException.errorCode) {
              case XAException.XA_HEURRB:
                  forgetHeuristicRollback(failedResourceHolder);
                  return true;

              case XAException.XAER_NOTA:
                  throw new BitronixXAException("resource reported XAER_NOTA when asked to rollback transaction branch", XAException.XA_HEURHAZ, xaException);

              case XAException.XA_HEURCOM:
              case XAException.XA_HEURHAZ:
              case XAException.XA_HEURMIX:
                  log.error("heuristic rollback is incompatible with the global state of this transaction - guilty: " + failedResourceHolder);
                  throw xaException;

              default:
                  return false;
          }
      }

    private static void forgetHeuristicRollback(XAResourceHolderState resourceHolder) {
        if (log.isDebugEnabled()) log.debug("handling heuristic rollback on resource " + resourceHolder.getXAResource());
        try {
            resourceHolder.getXAResource().forget(resourceHolder.getXid());
            if (log.isDebugEnabled()) log.debug("forgotten heuristically rolled back resource " + resourceHolder.getXAResource());
        } catch (XAException ex) {
            log.error("cannot forget " + resourceHolder.getXid() + " assigned to " + resourceHolder.getXAResource() + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
        }
    }


    private static class RollbackJob implements Runnable {
        private BitronixTransaction transaction;
        private XAResourceHolderState resourceHolder;
        private XAException xaException;
        private RuntimeException runtimeException;
        private TransactionTimeoutException transactionTimeoutException;
        private Object future;

        public RollbackJob(BitronixTransaction transaction, XAResourceHolderState resourceHolder) {
            this.transaction = transaction;
            this.resourceHolder = resourceHolder;
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

        public TransactionTimeoutException getTransactionTimeoutException() {
            return transactionTimeoutException;
        }

        public void run() {
            try {
                rollbackResource(transaction, resourceHolder);
            } catch (RuntimeException ex) {
                runtimeException = ex;
            } catch (XAException ex) {
                xaException = ex;
            } catch (TransactionTimeoutException ex) {
                transactionTimeoutException = ex;
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
