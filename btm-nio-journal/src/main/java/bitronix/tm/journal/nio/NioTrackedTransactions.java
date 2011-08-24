/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Juergen Kellerer.
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

package bitronix.tm.journal.nio;

import bitronix.tm.utils.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.transaction.Status.*;

/**
 * A tracker for dangling transactions (= transactions that need to be considered in rollovers and during Recoverer runs).
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioTrackedTransactions implements NioJournalConstants {

    private static final Logger log = LoggerFactory.getLogger(NioTrackedTransactions.class);

    private static boolean isFinalStatus(int status) {
        return status == STATUS_COMMITTED || status == STATUS_ROLLEDBACK;
    }

    private static boolean isTrackedRecordInValidStatus(int trackedStatus, int status) {
        switch (status) {
            case STATUS_COMMITTED:
                return trackedStatus == STATUS_COMMITTING;
            case STATUS_ROLLEDBACK:
                return trackedStatus == STATUS_ROLLING_BACK;
            case STATUS_PREPARED:
                return trackedStatus == STATUS_PREPARING;
        }
        return true;
    }

    private static boolean isTrackableStatus(int status) {
        return status != STATUS_NO_TRANSACTION &&
                status != STATUS_MARKED_ROLLBACK &&
                status != STATUS_UNKNOWN;
    }

    private final ConcurrentMap<Uid, NioJournalRecord> tracked = new ConcurrentHashMap<Uid, NioJournalRecord>();

    /**
     * Track the given transaction log record entry.
     *
     * @param record the record to track.
     */
    public void track(final NioJournalRecord record) {
        track(record.getStatus(), record.getGtrid(), record);
    }


    /**
     * Track the given transaction information.
     *
     * @param status the TX status.
     * @param gtrid  the global transaction id.
     * @param record the record to track.
     */
    public void track(final int status, final Uid gtrid, final NioJournalRecord record) {
        if (status == STATUS_UNKNOWN)
            return;

        if (isFinalStatus(status)) {
            final NioJournalRecord existing = tracked.get(gtrid);
            if (existing != null) {
                if (!isTrackedRecordInValidStatus(existing.getStatus(), status)) {
                    log.warn("Found a violation in the logged tx journal record " + record + ". The transaction " + gtrid + " was set to status " +
                            TRANSACTION_LONG_STATUS_STRINGS.get(status) + " on resources " + record.getUniqueNames() + " though the tracked status is " +
                            TRANSACTION_LONG_STATUS_STRINGS.get(existing.getStatus()) + " on resources " + existing.getUniqueNames());
                }

                existing.removeUniqueNamesFromRecord(record);
                if (existing.getUniqueNamesCount() == 0) {
                    tracked.remove(gtrid);
                    if (log.isDebugEnabled()) { log.debug("No longer tracking transaction '" + record + "', was '" + existing + "' before"); }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Changed status on transaction bits '" + record.getUniqueNames() + "' in '" + existing + "', from '" + record + "'");
                    }
                }
            }
        } else if (isTrackableStatus(status)) {
            final NioJournalRecord removed = tracked.put(gtrid, record);

            // Note: Merging names between removed and record is not required as the external implementation
            // should not provide a subset of names unless isFinalStatus returns true. Logging an error if the case still happens.
            if (removed != null && !removed.isUniqueNamesContainedInRecord(record)) {
                log.error("The unique names describing the TX members changed at an invalid TX status of " + TRANSACTION_LONG_STATUS_STRINGS.get(status) +
                        ", when tracking updates to the transaction " + gtrid + " inside the journal. (names had been " + removed.getUniqueNames() +
                        "at status " + TRANSACTION_LONG_STATUS_STRINGS.get(removed.getStatus()) + " and were now set to " + record.getUniqueNames() + ")");
            }

            if (log.isDebugEnabled()) { log.debug("Tracking pending transaction '" + record + "', was '" + removed + "' before"); }
        } else
            tracked.remove(gtrid);
    }

    /**
     * Purges entries that exceeded the maximum lifetime that transactions are tracked.
     */
    public void purgeTransactionsExceedingLifetime() {
        final long now = System.currentTimeMillis();
        for (Iterator<NioJournalRecord> i = tracked.values().iterator(); i.hasNext(); ) {
            NioJournalRecord journalRecord = i.next();

            final long age = now - journalRecord.getTime();
            if (age > TRANSACTION_MAX_LIFETIME) {
                log.warn("The maximum lifetime of " + MILLISECONDS.toHours(TRANSACTION_MAX_LIFETIME) + " hours was exceeded " +
                        "(TX age is " + MILLISECONDS.toHours(age) + " hours). Discarding dangling transaction " + journalRecord);
                i.remove();
            }
        }
    }

    public void clear() {
        tracked.clear();
    }

    public int size() {
        return tracked.size();
    }

    /**
     * Returns an unmodifiable map of tracked transactions.
     *
     * @return an unmodifiable map of tracked transactions.
     */
    public Map<Uid, NioJournalRecord> getTracked() {
        return Collections.unmodifiableMap(tracked);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NioTrackedTransactions{" +
                "tracked=" + tracked.size() +
                '}';
    }
}
