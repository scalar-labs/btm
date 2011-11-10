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
package bitronix.tm.recovery;

import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.Decoder;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Status;
import javax.transaction.xa.XAException;

/**
 * Incremental resource recoverer.
 *
 * @author lorban
 */
public class IncrementalRecoverer {

    private final static Logger log = LoggerFactory.getLogger(IncrementalRecoverer.class);

    /**
     * Run incremental recovery on the specified resource.
     * @param xaResourceProducer the resource to recover.
     * @throws RecoveryException when an error preventing recovery happens.
     */
    public static void recover(XAResourceProducer xaResourceProducer) throws RecoveryException {
        String uniqueName = xaResourceProducer.getUniqueName();
        if (log.isDebugEnabled()) log.debug("start of incremental recovery on resource " + uniqueName);

        try {
            XAResourceHolderState xaResourceHolderState = xaResourceProducer.startRecovery();
            boolean success = true;
            Set<BitronixXid> xids = RecoveryHelper.recover(xaResourceHolderState);
            if (log.isDebugEnabled()) log.debug(xids.size() + " dangling transaction(s) found on resource");
            Map danglingRecords = TransactionManagerServices.getJournal().collectDanglingRecords();
            if (log.isDebugEnabled()) log.debug(danglingRecords.size() + " dangling transaction(s) found in journal");

            int commitCount = 0;
            int rollbackCount = 0;
            for (BitronixXid xid : xids) {
                Uid gtrid = xid.getGlobalTransactionIdUid();

                TransactionLogRecord tlog = (TransactionLogRecord) danglingRecords.get(gtrid);
                if (tlog != null) {
                    if (log.isDebugEnabled()) log.debug("committing " + xid);
                    success &= RecoveryHelper.commit(xaResourceHolderState, xid);
                    updateJournal(xid.getGlobalTransactionIdUid(), uniqueName, Status.STATUS_COMMITTED);
                    commitCount++;
                } else {
                    if (log.isDebugEnabled()) log.debug("rolling back " + xid);
                    success &= RecoveryHelper.rollback(xaResourceHolderState, xid);
                    updateJournal(xid.getGlobalTransactionIdUid(), uniqueName, Status.STATUS_ROLLEDBACK);
                    rollbackCount++;
                }
            }

            // if recovery isn't successful we don't mark the resource as failed: heuristics might have happened
            // but communication with the resouce is working.
            if (!success)
                throw new RecoveryException("error recovering resource '" + uniqueName + "' due to an incompatible heuristic decision");

            xaResourceProducer.setFailed(false);

            log.info("incremental recovery committed " + commitCount + " dangling transaction(s) and rolled back " + rollbackCount +
                    " aborted transaction(s) on resource [" + uniqueName + "]" +
                    ((TransactionManagerServices.getConfiguration().isCurrentNodeOnlyRecovery()) ? " (restricted to serverId '" + TransactionManagerServices.getConfiguration().getServerId() + "')" : ""));

        } catch (XAException ex) {
            xaResourceProducer.setFailed(true);
            throw new RecoveryException("failed recovering resource " + uniqueName, ex);
        } catch (IOException ex) {
            xaResourceProducer.setFailed(true);
            throw new RecoveryException("failed recovering resource " + uniqueName, ex);
        } catch (RuntimeException ex) {
            xaResourceProducer.setFailed(true);
            throw new RecoveryException("failed recovering resource " + uniqueName, ex);
        } catch (RecoveryException ex) {
            xaResourceProducer.setFailed(true);
            throw ex;
        } finally {
            xaResourceProducer.endRecovery();
            if (log.isDebugEnabled()) log.debug("end of incremental recovery on resource " + uniqueName);
        }
    }

    private static void updateJournal(Uid gtrid, String uniqueName, int status) throws IOException {
        if (log.isDebugEnabled()) log.debug("updating journal, adding " + Decoder.decodeStatus(status) + " entry for [" + uniqueName + "] on GTRID [" +  gtrid + "]");
        Set<String> participatingUniqueNames = new HashSet<String>();
        participatingUniqueNames.add(uniqueName);
        TransactionManagerServices.getJournal().log(status, gtrid, participatingUniqueNames);
    }


}
