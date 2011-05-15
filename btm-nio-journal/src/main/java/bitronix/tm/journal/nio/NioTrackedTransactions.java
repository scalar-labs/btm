/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Bitronix Software.
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

import javax.transaction.Status;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A tracker for dangling transactions (= transactions that need to be considered in rollovers
 * and during Recoverer runs).
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioTrackedTransactions implements NioJournalConstants {

    private static final Logger log = LoggerFactory.getLogger(NioTrackedTransactions.class);

    private static boolean isFinalStatus(int status) {
        return status == Status.STATUS_COMMITTED ||
                status == Status.STATUS_ROLLEDBACK ||
                status == Status.STATUS_NO_TRANSACTION ||
                status == Status.STATUS_UNKNOWN;
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
        if (isFinalStatus(status)) {
            final NioJournalRecord existing = tracked.get(gtrid);
            if (existing != null) {
                existing.removeUniqueNames(record.getUniqueNames());
                if (existing.getUniqueNames().isEmpty()) {
                    tracked.remove(gtrid);
                    if (log.isTraceEnabled())
                        log.trace("No longer tracking transaction '{}', was '{}' before", record, existing);
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("Changed status on transaction bits '{}' in '{}', from '{}'",
                                new Object[]{record.getUniqueNames(), existing, record});
                    }
                }
            }
        } else {
            final NioJournalRecord removed = tracked.put(gtrid, record);

            // Note: Merging names between removed and record is not required as the external implementation
            // should not change the unique names unless isFinalStatus returns true. Logging an error if the
            // case still happens.
            if (removed != null && !removed.getUniqueNames().equals(record.getUniqueNames())) {
                log.error("The unique names describing the TX members changed at an invalid TX status of {}, " +
                        "when tracking updates to the transaction {} inside the journal. (names had been " +
                        "{} and were now set to {})", new Object[]{status, gtrid,
                        removed.getUniqueNames(), record.getUniqueNames()});

            }

            if (log.isTraceEnabled())
                log.trace("Tracking pending transaction '{}', was '{}' before", record, removed);
        }
    }

    /**
     * Purges entries that exceeded the maximum lifetime that transactions are tracked.
     */
    public void purgeTransactionsExceedingLifetime() {
        final long now = System.currentTimeMillis();
        for (Iterator<NioJournalRecord> i = tracked.values().iterator(); i.hasNext();) {
            NioJournalRecord journalRecord = i.next();
            final long age = now - journalRecord.getTime();
            if (age > TRANSACTION_MAX_LIFETIME) {
                log.warn("Max life time of {}m exeeded (age was {}m). Discarding dangling transaction {}",
                        new Object[]{TimeUnit.MILLISECONDS.toMinutes(TRANSACTION_MAX_LIFETIME),
                                TimeUnit.MILLISECONDS.toMinutes(age), journalRecord});
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
