package bitronix.tm.recovery;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.*;
import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.resource.ResourceLoader;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAResourceProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.IOException;
import java.util.*;

/**
 * Recovery process implementation. Here is Mike Spille's description of XA recovery:
 * <p>
 * Straight Line Recovery:
 * <ul>
 *   <li>1. Find transactions that the TM considers dangling and unresolved</li>
 *   <li>2. Find and reconstitute any {@link XAResource}s which were being used when chunk blowing occured.</li>
 *   <li>3. Call the <code>recover()</code> method on each of these {@link XAResource}s.</li>
 *   <li>4. Throw out any {@link Xid}'s in the {@link XAResource}' recover lists which are not owned by this TM.</li>
 *   <li>5. Correlate {@link Xid}'s that the TM knows about with remaining {@link Xid}'s that the {@link XAResource}s
 *          reported.</li>
 *   <li>6. For {@link XAResource} {@link Xid}'s that match the global transaction ID which the TM found dangling with
 *          a "Committing..." record, call <code>commit()</code> on those {@link XAResource}s for those {@link Xid}s.</li>
 *   <li>7. For {@link XAResource} {@link Xid}'s that do not match any dangling "Committing..." records, call
 *          <code>rollback()</code> on those {@link XAResource}s for those {@link Xid}s.</li>
 * </ul>
 * Exceptional conditions:
 * <ul>
 *   <li>1. For any <code>rollback()</code> calls from step 6 which reported a Heuristic Commit, you are in danger or
 *          doubt, so run in circles, scream and shout.</li>
 *   <li>2. For any <code>commit()</code> calls from step 7 which reported a Heuristic Rollback, you are in danger or
 *          doubt, so run in circles, scream and shout.</li>
 *   <li>3. For any resource you can't reconstitute in in step #2, or who fails on recover in step #3, or who reports
 *          anything like an XAER_RMFAILURE in step 6 or step 7, keep trying to contact them in some implementation
 *          defined manner.</li>
 *   <li>4. For any heuristic outcome you see reported from an XAResource, call <code>forget()</code> for that
 *          {@link XAResource}/{@link Xid} pair so that the resource can stop holding onto a reference to that transaction</li>
 * </ul>
 * </p>
 * <p>To achieve this, {@link Recoverer} must have access to all previously used resources, even if the journal contains
 * no trace of some of them. There are two ways of achieving this: either you use the {@link ResourceLoader} to configure
 * all your resources and everything will be working automatically or by making sure resources are created before
 * the transaction manager starts.</p>
 * <p>Those are the three steps of the Bitronix implementation:
 * <ul>
 *   <li>call <code>recover()</code> on all known resources (Mike's steps 1 to 5)</li>
 *   <li>commit dangling COMMITTING transactions (Mike's step 6)</li>
 *   <li>rollback any remaining recovered transaction (Mike's step 7)</li>
 * </ul></p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @see ResourceLoader
 * @author lorban
 */
public class Recoverer implements Runnable, Service, RecovererMBean {

    private final static Logger log = LoggerFactory.getLogger(Recoverer.class);

    private Map registeredResources = new HashMap();
    private Map recoveredXidSets = new HashMap();

    private Exception completionException;
    private int committedCount;
    private int rolledbackCount;


    public Recoverer() {
        ManagementRegistrar.register("bitronix.tm:type=Recoverer", this);
    }

    public void shutdown() {
        ManagementRegistrar.unregister("bitronix.tm:type=Recoverer");
    }

    /**
     * Run the recovery process. This method is automatically called by the transaction manager, you should never
     * call it manually.
     */
    public void run() {
        try {
            if (ResourceRegistrar.getResourcesUniqueNames().size() == 0) {
                log.warn("no recoverable resource found. Please check Resource Loader configuration or make sure you " +
                        "created resources before starting the transaction manager.");
                return;
            }

            committedCount = 0;
            rolledbackCount = 0;

            // Query resources from ResourceRegistrar
            Iterator it = ResourceRegistrar.getResourcesUniqueNames().iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                registeredResources.put(name, ResourceRegistrar.get(name));
            }

            // 1. call recover on all known resources
            recoverAllResources();

            // 2. commit dangling COMMITTING transactions
            Set committedGtrids = commitDanglingTransactions();
            committedCount = committedGtrids.size();

            // 3. rollback any remaining recovered transaction
            rolledbackCount = rollbackAbortedTransactions(committedGtrids);

            log.info("recovery committed " + committedCount + " dangling transaction(s) and rolled back " + rolledbackCount + " aborted transaction(s) on resource(s) " + getResourcesUniqueNames());
            this.completionException = null;
        } catch (Exception ex) {
            this.completionException = ex;
            if (log.isDebugEnabled()) log.debug("recovery failed, resource(s): " + getResourcesUniqueNames(), ex);
        }
        finally {
            recoveredXidSets.clear();
            registeredResources.clear();
        }
    }

    /**
     * Get the exception reported when recovery failed.
     * @return the exception that made recovery fail or null if last recovery execution was successful.
     */
    public Exception getCompletionException() {
        return completionException;
    }

    /**
     * Get the amount of transactions committed during the last recovery run.
     * @return the amount of committed transactions.
     */
    public int getCommittedCount() {
        return committedCount;
    }

    /**
     * Get the amount of transactions rolled back during the last recovery run.
     * @return the amount of rolled back transactions.
     */
    public int getRolledbackCount() {
        return rolledbackCount;
    }

    /**
     * Recover all configured resources and fill the <code>recoveredXidSets</code> with all recovered XIDs.
     * Step 1.
     */
    private void recoverAllResources() {
        Iterator it = registeredResources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String uniqueName = (String) entry.getKey();
            XAResourceProducer producer = (XAResourceProducer) entry.getValue();

            try {
                if (log.isDebugEnabled()) log.debug("performing recovery on " + uniqueName);
                Set xids = recover(producer);
                if (log.isDebugEnabled()) log.debug("recovered " + xids.size() + " XID(s) from resource " + uniqueName);
                recoveredXidSets.put(uniqueName, xids);
            } catch (Exception ex) {
                throw new RecoveryException("error running recovery on resource " + uniqueName, ex);
            }
        }
        if (log.isDebugEnabled()) log.debug(registeredResources.size() + " resource(s) recovered");
    }

    /**
     * Run the recovery process on the target resource.
     * Step 1.
     * @return a Set of BitronixXids.
     * @param producer the {@link XAResourceProducer} to recover.
     * @throws javax.transaction.xa.XAException if {@link XAResource#recover(int)} call fails.
     */
    private Set recover(XAResourceProducer producer) throws XAException {
        if (producer == null)
            throw new NullPointerException("recoverable resource cannot be null");

        Set xids = new HashSet();

        if (log.isDebugEnabled()) log.debug("running recovery on " + producer);

        try {
            XAResourceHolderState xaResourceHolderState = producer.startRecovery();
            int xidCount = recover(xaResourceHolderState, xids, XAResource.TMSTARTRSCAN);
            if (log.isDebugEnabled()) log.debug("STARTRSCAN recovered " + xidCount + " xid(s) on " + producer.getUniqueName());

            while (xidCount > 0) {
                xidCount = recover(xaResourceHolderState, xids, XAResource.TMNOFLAGS);
                if (log.isDebugEnabled()) log.debug("NOFLAGS recovered " + xidCount + " xid(s) on " + producer.getUniqueName());
            }

            xidCount = recover(xaResourceHolderState, xids, XAResource.TMENDRSCAN);
            if (log.isDebugEnabled()) log.debug("ENDRSCAN recovered " + xidCount + " xid(s) on " + producer.getUniqueName());
        } finally {
            producer.endRecovery();
        }

        return xids;
    }

    /**
     * Call recovery on the resource and fill the alreadyRecoveredXids Set with recovered BitronixXids.
     * Step 1.
     * @return the amount of recovered {@link Xid}.
     * @param resourceHolderState the {@link XAResourceHolderState} to recover.
     * @param alreadyRecoveredXids a set of {@link Xid}s already recovered from this resource in this recovery session.
     * @param flags any combination of {@link XAResource#TMSTARTRSCAN}, {@link XAResource#TMNOFLAGS} or {@link XAResource#TMENDRSCAN}.
     * @throws javax.transaction.xa.XAException if {@link XAResource#recover(int)} call fails.
     */
    private int recover(XAResourceHolderState resourceHolderState, Set alreadyRecoveredXids, int flags) throws XAException {
        Xid[] xids = resourceHolderState.getXAResource().recover(flags);
        if (xids == null)
            return 0;

        Set freshlyRecoveredXids = new HashSet();
        for (int i = 0; i < xids.length; i++) {
            Xid xid = xids[i];
            if (xid.getFormatId() == BitronixXid.FORMAT_ID) {
                BitronixXid bitronixXid = new BitronixXid(xid);
                if (!alreadyRecoveredXids.contains(bitronixXid)) {
                    if (!freshlyRecoveredXids.contains(bitronixXid)) {
                        if (log.isDebugEnabled()) log.debug("recovered " + bitronixXid);
                        freshlyRecoveredXids.add(bitronixXid);
                    }
                    else {
                        log.warn("resource " + resourceHolderState.getUniqueName() + " recovered two identical XIDs within the same recover call: " + bitronixXid);
                    }
                }
                else {
                    if (log.isDebugEnabled()) log.debug("already recovered XID " + bitronixXid + ", skipping it");
                }
            }
            else {
                if (log.isDebugEnabled()) log.debug("skipped non-bitronix XID " + xid + "(format ID: " + xid.getFormatId() +
                    " GTRID: " + new Uid(xid.getGlobalTransactionId()) + "BQUAL: " + new Uid(xid.getBranchQualifier()) + ")");
            }
        } // for i < xids.length

        alreadyRecoveredXids.addAll(freshlyRecoveredXids);
        return freshlyRecoveredXids.size();
    }


    /**
     * Commit transactions that have a dangling COMMITTING record in the journal.
     * Step 2.
     * @return a Set of all committed GTRIDs encoded as strings.
     * @throws java.io.IOException if there is an I/O error reading the journal.
     */
    private Set commitDanglingTransactions() throws IOException {
        Set committedGtrids = new HashSet();

        Map danglingRecords = TransactionManagerServices.getJournal().collectDanglingRecords();
        if (log.isDebugEnabled()) log.debug("found " + danglingRecords.size() + " dangling record(s) in journal");
        Iterator it = danglingRecords.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Uid gtrid = (Uid) entry.getKey();
            TransactionLogRecord tlog = (TransactionLogRecord) entry.getValue();

            Set uniqueNames = tlog.getUniqueNames();
            Set danglingTransactions = getDanglingTransactionsInRecoveredXids(uniqueNames, tlog.getGtrid());
            if (log.isDebugEnabled()) log.debug("committing dangling transaction with GTRID " + gtrid);
            commit(danglingTransactions);
            if (log.isDebugEnabled()) log.debug("committed dangling transaction with GTRID " + gtrid);
            committedGtrids.add(gtrid);

            if (log.isDebugEnabled()) log.debug("updating journal's transaction status to COMMITTED");
            TransactionManagerServices.getJournal().log(Status.STATUS_COMMITTED, tlog.getGtrid(), uniqueNames);
        }
        if (log.isDebugEnabled()) log.debug("committed " + committedGtrids.size() + " dangling transaction(s)");
        return committedGtrids;
    }

    /**
     * Return {@link DanglingTransaction}s with {@link Xid}s corresponding to the GTRID parameter found in resources
     * specified by their <code>uniqueName</code>s.
     * <code>recoverAllResources</code> must have been called before or else the returned list will always be empty.
     * Step 2.
     * @param uniqueNames a set of <code>uniqueName</code>s.
     * @param gtrid the GTRID to look for.
     * @return a set of {@link DanglingTransaction}s.
     */
    private Set getDanglingTransactionsInRecoveredXids(Set uniqueNames, Uid gtrid) {
        Set danglingTransactions = new HashSet();

        Iterator it = uniqueNames.iterator();
        while (it.hasNext()) {
            String uniqueName = (String) it.next();
            if (log.isDebugEnabled()) log.debug("finding dangling transaction(s) in recovered XID(s) of resource " + uniqueName);
            Set recoveredXids = (Set) recoveredXidSets.get(uniqueName);
            if (recoveredXids == null) {
                log.error("recovery haven't been run on resource named " + uniqueName + " found in the transaction log, " +
                        "please check ResourceLoader configuration file or make sure you manually created this resource " +
                        "before starting the transaction manager");
                continue;
            }

            Iterator it2 = recoveredXids.iterator();
            while (it2.hasNext()) {
                BitronixXid recoveredXid = (BitronixXid) it2.next();
                if (gtrid.equals(recoveredXid.getGlobalTransactionIdUid())) {
                    if (log.isDebugEnabled()) log.debug("found a recovered XID matching dangling log's GTRID " + gtrid + " in resource " + uniqueName);
                    danglingTransactions.add(new DanglingTransaction(uniqueName, recoveredXid));
                }
            } // while it2.hasNext()
        }

        return danglingTransactions;
    }

    /**
     * Commit all branches of a dangling transaction.
     * Step 2.
     * @param danglingTransactions a set of {@link DanglingTransaction}s to commit.
     */
    private void commit(Set danglingTransactions) {
        if (log.isDebugEnabled()) log.debug(danglingTransactions.size() + " branch(es) to commit");

        Iterator it = danglingTransactions.iterator();
        while (it.hasNext()) {
            DanglingTransaction danglingTransaction = (DanglingTransaction) it.next();
            Xid xid = danglingTransaction.getXid();
            String uniqueName = danglingTransaction.getUniqueName();

            if (log.isDebugEnabled()) log.debug("committing branch with XID " + xid + " on " + uniqueName);
            commit(uniqueName, xid);
        }
    }

    /**
     * Commit the specified branch of a dangling transaction.
     * Step 2.
     * @param uniqueName the unique name of the resource on which the commit should be done.
     * @param xid the {@link Xid} to commit.
     * @return true when commit was successful.
     */
    private boolean commit(String uniqueName, Xid xid) {
        XAResourceProducer producer = (XAResourceProducer) registeredResources.get(uniqueName);
        try {
            boolean success = true;
            boolean forget = false;
            XAResourceHolderState xaResourceHolderState = producer.startRecovery();
            try {
                xaResourceHolderState.getXAResource().commit(xid, false);
            } catch (XAException ex) {
                if (ex.errorCode == XAException.XAER_NOTA) {
                    log.error("unable to commit in-doubt branch on resource " + uniqueName + " - error=XAER_NOTA. Forgotten heuristic ?", ex);
                }
                else if (ex.errorCode == XAException.XA_HEURCOM) {
                    log.info("unable to commit in-doubt branch on resource " + uniqueName + " - error=" +
                            Decoder.decodeXAExceptionErrorCode(ex) + ". Heuristic decision compatible with the global state of this transaction.");
                    forget = true;
                }
                else if (ex.errorCode == XAException.XA_HEURHAZ || ex.errorCode == XAException.XA_HEURMIX || ex.errorCode == XAException.XA_HEURRB) {
                    log.error("unable to commit in-doubt branch on resource " + uniqueName + " - error=" +
                            Decoder.decodeXAExceptionErrorCode(ex) + ". Heuristic decision incompatible with the global state of this transaction !");
                    forget = true;
                    success = false;
                }
                else {
                    log.error("unable to commit in-doubt branch on resource " + uniqueName + " - error=" + Decoder.decodeXAExceptionErrorCode(ex) + ".", ex);
                    success = false;
                }
            }
            if (forget) {
                try {
                    if (log.isDebugEnabled()) log.debug("forgetting XID " + xid + " on resource " + uniqueName);
                    xaResourceHolderState.getXAResource().forget(xid);
                } catch (XAException ex) {
                    log.error("unable to forget XID " + xid + " on resource " + uniqueName + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
                }
            }
            return success;
        } finally {
            producer.endRecovery();
        }
    }

    /**
     * Rollback branches whose {@link Xid} has been recovered on the resource but hasn't been committed.
     * Those are the 'aborted' transactions of the Presumed Abort protocol.
     * Step 3.
     * @param committedGtrids a set of {@link Uid}s already committed on this resource.
     * @return the rolled back branches count.
     */
    private int rollbackAbortedTransactions(Set committedGtrids) {
        if (log.isDebugEnabled()) log.debug("rolling back aborted branch(es)");
        int rollbackCount = 0;
        Iterator it = recoveredXidSets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String uniqueName = (String) entry.getKey();
            Set recoveredXids = (Set) entry.getValue();

            if (log.isDebugEnabled()) log.debug("checking " + recoveredXids.size() + " branch(es) on " + uniqueName + " for rollback");
            int count = rollbackAbortedBranchesOfResource(uniqueName, recoveredXids, committedGtrids);
            if (log.isDebugEnabled()) log.debug("checked " + recoveredXids.size() + " branch(es) on " + uniqueName + " for rollback");
            rollbackCount += count;
        }

        if (log.isDebugEnabled()) log.debug("rolled back " + rollbackCount + " aborted branch(es)");
        return rollbackCount;
    }

    /**
     * Rollback aborted branches of the resource specified by uniqueName.
     * Step 3.
     * @param uniqueName the unique name of the resource on which to rollback branches.
     * @param recoveredXids a set of {@link BitronixXid} recovered on the reource.
     * @param committedGtrids a set of {@link Uid}s already committed on the resource.
     * @return the rolled back branches count.
     */
    private int rollbackAbortedBranchesOfResource(String uniqueName, Set recoveredXids, Set committedGtrids) {
        int abortedCount = 0;
        Iterator it = recoveredXids.iterator();
        while (it.hasNext()) {
            BitronixXid recoveredXid = (BitronixXid) it.next();
            if (committedGtrids.contains(recoveredXid.getGlobalTransactionIdUid())) {
                if (log.isDebugEnabled()) log.debug("XID has been committed, skipping rollback: " + recoveredXid + " on " + uniqueName);
                continue;
            }

            if (log.isDebugEnabled()) log.debug("rolling back in-doubt branch with XID " + recoveredXid + " on " + uniqueName);
            boolean success = rollback(uniqueName, recoveredXid);
            if (success)
                abortedCount++;
        }
        return abortedCount;
    }

    /**
     * Rollback the specified branch of a dangling transaction.
     * Step 3.
     * @param uniqueName the unique name of the resource on which to rollback branches.
     * @param recoveredXid the {@link BitronixXid} to rollback.
     * @return true when rollback was successful.
     */
    private boolean rollback(String uniqueName, BitronixXid recoveredXid) {
        XAResourceProducer producer = (XAResourceProducer) registeredResources.get(uniqueName);
        try {
            boolean success = true;
            boolean forget = false;
            XAResourceHolderState xaResourceHolderState = producer.startRecovery();
            try {
                xaResourceHolderState.getXAResource().rollback(recoveredXid);
            } catch (XAException ex) {
                if (ex.errorCode == XAException.XAER_NOTA) {
                    log.error("unable to rollback aborted in-doubt branch on resource " + uniqueName + " - error=XAER_NOTA. Forgotten heuristic ?", ex);
                }
                else if (ex.errorCode == XAException.XA_HEURRB) {
                    log.info("unable to rollback aborted in-doubt branch on resource " + uniqueName + " - error=" +
                            Decoder.decodeXAExceptionErrorCode(ex) + ". Heuristic decision compatible with the global state of this transaction.");
                    forget = true;
                }
                else if (ex.errorCode == XAException.XA_HEURHAZ || ex.errorCode == XAException.XA_HEURMIX || ex.errorCode == XAException.XA_HEURCOM) {
                    log.error("unable to rollback aborted in-doubt branch on resource " + uniqueName + " - error=" +
                            Decoder.decodeXAExceptionErrorCode(ex) + ". Heuristic decision incompatible with the global state of this transaction !");
                    forget = true;
                    success = false;
                }
                else {
                    log.error("unable to rollback aborted in-doubt branch on resource " + uniqueName + " - error=" +
                            Decoder.decodeXAExceptionErrorCode(ex) + ".", ex);
                    success = false;
                }
            }
            if (forget) {
                try {
                    if (log.isDebugEnabled()) log.debug("forgetting XID " + recoveredXid + " on resource " + uniqueName);
                    xaResourceHolderState.getXAResource().forget(recoveredXid);
                } catch (XAException ex) {
                    log.error("unable to forget XID " + recoveredXid + " on resource " + uniqueName + ", error=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
                }
            }
            return success;
        } finally {
            producer.endRecovery();
        }
    }

    /**
     * Build a string with comma-separated resources unique names.
     * @return the string.
     */
    private String getResourcesUniqueNames() {
        StringBuffer resourcesUniqueNames = new StringBuffer();
        Iterator it = registeredResources.keySet().iterator();
        while (it.hasNext()) {
            String uniqueName = (String) it.next();
            resourcesUniqueNames.append(uniqueName);
            if (it.hasNext())
                resourcesUniqueNames.append(", ");
        }
        return resourcesUniqueNames.toString();
    }

}
