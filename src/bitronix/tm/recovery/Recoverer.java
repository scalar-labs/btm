package bitronix.tm.recovery;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.ManagementRegistrar;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.Service;
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
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
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
            committedCount = 0;
            rolledbackCount = 0;
            long oldestTransactionTimestamp = Long.MAX_VALUE;

            // Query resources from ResourceRegistrar
            synchronized (ResourceRegistrar.class) {
                Iterator it = ResourceRegistrar.getResourcesUniqueNames().iterator();
                while (it.hasNext()) {
                    String name = (String) it.next();
                    registeredResources.put(name, ResourceRegistrar.get(name));
                }

                if (TransactionManagerServices.isTransactionManagerRunning()) {
                    oldestTransactionTimestamp = TransactionManagerServices.getTransactionManager().getOldestInFlightTransactionTimestamp();
                }
            }

            Map danglingRecords = TransactionManagerServices.getJournal().collectDanglingRecords();

            // 1. call recover on all known resources
            recoverAllResources();

            // 2. commit dangling COMMITTING transactions
            Set committedGtrids = commitDanglingTransactions(oldestTransactionTimestamp, danglingRecords);
            committedCount = committedGtrids.size();

            // 3. rollback any remaining recovered transaction
            rolledbackCount = rollbackAbortedTransactions(oldestTransactionTimestamp, committedGtrids);

            log.info("recovery committed " + committedCount + " dangling transaction(s) and rolled back " + rolledbackCount +
                    " aborted transaction(s) on " + registeredResources.size() + " resource(s) [" + getRegisteredResourcesUniqueNames() + "]" +
                    ((TransactionManagerServices.getConfiguration().isCurrentNodeOnlyRecovery()) ? " (restricted to serverId '" + TransactionManagerServices.getConfiguration().getServerId() + "')" : ""));

            this.completionException = null;
        } catch (Exception ex) {
            this.completionException = ex;
            log.warn("recovery failed, registered resource(s): " + getRegisteredResourcesUniqueNames(), ex);
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
        Iterator it = new HashMap(registeredResources).entrySet().iterator(); // a cloned registeredResources Map must be iterated as the original one can be modified in the loop
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String uniqueName = (String) entry.getKey();
            XAResourceProducer producer = (XAResourceProducer) entry.getValue();

            try {
                if (log.isDebugEnabled()) log.debug("performing recovery on " + uniqueName);
                Set xids = recover(producer);
                if (log.isDebugEnabled()) log.debug("recovered " + xids.size() + " XID(s) from resource " + uniqueName);
                recoveredXidSets.put(uniqueName, xids);
                producer.setFailed(false);
            } catch (XAException ex) {
                producer.setFailed(true);
                registeredResources.remove(uniqueName);
                log.warn("error running recovery on resource '" + uniqueName + "', resource marked as failed (background recoverer will retry recovery) (error=" + Decoder.decodeXAExceptionErrorCode(ex) + ")", ex);
            } catch (Exception ex) {
                producer.setFailed(true);
                registeredResources.remove(uniqueName);
                log.warn("error running recovery on resource '" + uniqueName + "', resource marked as failed (background recoverer will retry recovery)", ex);
            }
        }
    }

    /**
     * Run the recovery process on the target resource.
     * Step 1.
     * @return a Set of BitronixXids.
     * @param producer the {@link XAResourceProducer} to recover.
     * @throws javax.transaction.xa.XAException if {@link XAResource#recover(int)} call fails.
     * @throws RecoveryException if an error preventing recovery happened.
     */
    private Set recover(XAResourceProducer producer) throws XAException, RecoveryException {
        if (producer == null)
            throw new IllegalArgumentException("recoverable resource cannot be null");

        if (log.isDebugEnabled()) log.debug("running recovery on " + producer);
        XAResourceHolderState xaResourceHolderState = producer.startRecovery();
        try {
            return RecoveryHelper.recover(xaResourceHolderState);
        } finally {
            producer.endRecovery();
        }
    }

    /**
     * Commit transactions that have a dangling COMMITTING record in the journal.
     * Step 2.
     * @param oldestTransactionTimestamp the timestamp of the oldest transaction still in-flight.
     * @param danglingRecords a Map using Uid objects GTRID as key and {@link TransactionLogRecord} as value.
     * @return a Set of all committed GTRIDs encoded as strings.
     * @throws java.io.IOException if there is an I/O error reading the journal.
     * @throws RecoveryException if an error preventing recovery happened.
     */
    private Set commitDanglingTransactions(long oldestTransactionTimestamp, Map danglingRecords) throws IOException, RecoveryException {
        Set committedGtrids = new HashSet();

        if (log.isDebugEnabled()) log.debug("found " + danglingRecords.size() + " dangling record(s) in journal");
        Iterator it = danglingRecords.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Uid gtrid = (Uid) entry.getKey();
            TransactionLogRecord tlog = (TransactionLogRecord) entry.getValue();

            Set uniqueNames = tlog.getUniqueNames();
            Set danglingTransactions = getDanglingTransactionsInRecoveredXids(uniqueNames, tlog.getGtrid());

            long txTimestamp = gtrid.extractTimestamp();
            if (log.isDebugEnabled()) log.debug("recovered XID timestamp: " + txTimestamp + " - oldest in-flight TX timestamp: " + oldestTransactionTimestamp);

            if (txTimestamp < oldestTransactionTimestamp) {
                if (log.isDebugEnabled()) log.debug("committing dangling transaction with GTRID " + gtrid);
                commit(danglingTransactions);
                if (log.isDebugEnabled()) log.debug("committed dangling transaction with GTRID " + gtrid);
                committedGtrids.add(gtrid);

                Set participatingUniqueNames = filterParticipatingUniqueNamesInRecoveredXids(uniqueNames);

                if (participatingUniqueNames.size() > 0) {
                    if (log.isDebugEnabled()) log.debug("updating journal's transaction with GTRID " + gtrid + " status to COMMITTED for names [" + buildUniqueNamesString(participatingUniqueNames) + "]");
                    TransactionManagerServices.getJournal().log(Status.STATUS_COMMITTED, tlog.getGtrid(), participatingUniqueNames);
                }
                else {
                    if (log.isDebugEnabled()) log.debug("not updating journal's transaction with GTRID " + gtrid + " status to COMMITTED as no resource could be found (incremental recovery will need to clean this)");
                    committedGtrids.remove(gtrid);
                }
            }
            else {
                if (log.isDebugEnabled()) log.debug("skipping in-flight transaction with GTRID " + gtrid);
            }
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
                if (log.isDebugEnabled()) log.debug("resource " + uniqueName + " did not recover, skipping commit");
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

    private Set filterParticipatingUniqueNamesInRecoveredXids(Set uniqueNames) {
        Set recoveredUniqueNames = new HashSet();

        Iterator it = uniqueNames.iterator();
        while (it.hasNext()) {
            String uniqueName = (String) it.next();
            if (log.isDebugEnabled()) log.debug("finding dangling transaction(s) in recovered XID(s) of resource " + uniqueName);
            Set recoveredXids = (Set) recoveredXidSets.get(uniqueName);
            if (recoveredXids == null) {
                if (log.isDebugEnabled()) log.debug("cannot find resource '" + uniqueName + "' present in the journal, leaving it for incremental recovery");
            }
            else {
                recoveredUniqueNames.add(uniqueName);
            }
        }

        return recoveredUniqueNames;
    }

    /**
     * Commit all branches of a dangling transaction.
     * Step 2.
     * @param danglingTransactions a set of {@link DanglingTransaction}s to commit.
     * @throws RecoveryException if an error preventing recovery happened.
     */
    private void commit(Set danglingTransactions) throws RecoveryException {
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
     * @throws RecoveryException if an error preventing recovery happened.
     */
    private boolean commit(String uniqueName, Xid xid) throws RecoveryException {
        XAResourceProducer producer = (XAResourceProducer) registeredResources.get(uniqueName);
        try {
            XAResourceHolderState xaResourceHolderState = producer.startRecovery();
            return RecoveryHelper.commit(xaResourceHolderState, xid);
        } finally {
            producer.endRecovery();
        }
    }

    /**
     * Rollback branches whose {@link Xid} has been recovered on the resource but hasn't been committed.
     * Those are the 'aborted' transactions of the Presumed Abort protocol.
     * Step 3.
     * @param oldestTransactionTimestamp the timestamp of the oldest transaction still in-flight.
     * @param committedGtrids a set of {@link Uid}s already committed on this resource.
     * @return the rolled back branches count.
     * @throws RecoveryException if an error preventing recovery happened.
     */
    private int rollbackAbortedTransactions(long oldestTransactionTimestamp, Set committedGtrids) throws RecoveryException {
        if (log.isDebugEnabled()) log.debug("rolling back aborted branch(es)");
        int rollbackCount = 0;
        Iterator it = recoveredXidSets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String uniqueName = (String) entry.getKey();
            Set recoveredXids = (Set) entry.getValue();

            if (log.isDebugEnabled()) log.debug("checking " + recoveredXids.size() + " branch(es) on " + uniqueName + " for rollback");
            int count = rollbackAbortedBranchesOfResource(oldestTransactionTimestamp, uniqueName, recoveredXids, committedGtrids);
            if (log.isDebugEnabled()) log.debug("checked " + recoveredXids.size() + " branch(es) on " + uniqueName + " for rollback");
            rollbackCount += count;
        }

        if (log.isDebugEnabled()) log.debug("rolled back " + rollbackCount + " aborted branch(es)");
        return rollbackCount;
    }

    /**
     * Rollback aborted branches of the resource specified by uniqueName.
     * Step 3.
     * @param oldestTransactionTimestamp the timestamp of the oldest transaction still in-flight.
     * @param uniqueName the unique name of the resource on which to rollback branches.
     * @param recoveredXids a set of {@link BitronixXid} recovered on the reource.
     * @param committedGtrids a set of {@link Uid}s already committed on the resource.
     * @return the rolled back branches count.
     * @throws RecoveryException if an error preventing recovery happened.
     */
    private int rollbackAbortedBranchesOfResource(long oldestTransactionTimestamp, String uniqueName, Set recoveredXids, Set committedGtrids) throws RecoveryException {
        int abortedCount = 0;
        Iterator it = recoveredXids.iterator();
        while (it.hasNext()) {
            BitronixXid recoveredXid = (BitronixXid) it.next();
            if (committedGtrids.contains(recoveredXid.getGlobalTransactionIdUid())) {
                if (log.isDebugEnabled()) log.debug("XID has been committed, skipping rollback: " + recoveredXid + " on " + uniqueName);
                continue;
            }

            long txTimestamp = recoveredXid.getGlobalTransactionIdUid().extractTimestamp();
            if (log.isDebugEnabled()) log.debug("recovered XID timestamp: " + txTimestamp + " - oldest in-flight TX timestamp: " + oldestTransactionTimestamp);
            if (txTimestamp >= oldestTransactionTimestamp) {
                if (log.isDebugEnabled()) log.debug("skipping XID of in-flight transaction: " + recoveredXid);
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
     * @param xid the {@link Xid} to rollback.
     * @return true when rollback was successful.
     * @throws RecoveryException if an error preventing recovery happened.
     */
    private boolean rollback(String uniqueName, Xid xid) throws RecoveryException {
        XAResourceProducer producer = (XAResourceProducer) registeredResources.get(uniqueName);
        if (producer == null) {
            if (log.isDebugEnabled()) log.debug("resource " + uniqueName + " has not recovered, skipping rollback");
            return false;
        }

        try {
            XAResourceHolderState xaResourceHolderState = producer.startRecovery();
            return RecoveryHelper.rollback(xaResourceHolderState, xid);
        } finally {
            producer.endRecovery();
        }
    }

    /**
     * Build a string with comma-separated resources unique names.
     * @return the string.
     */
    private String getRegisteredResourcesUniqueNames() {
        return buildUniqueNamesString(registeredResources.keySet());
    }

    private static String buildUniqueNamesString(Set uniqueNames) {
        StringBuffer resourcesUniqueNames = new StringBuffer();
        Iterator it = uniqueNames.iterator();
        while (it.hasNext()) {
            String uniqueName = (String) it.next();
            resourcesUniqueNames.append(uniqueName);
            if (it.hasNext())
                resourcesUniqueNames.append(", ");
        }
        return resourcesUniqueNames.toString();
    }

}
