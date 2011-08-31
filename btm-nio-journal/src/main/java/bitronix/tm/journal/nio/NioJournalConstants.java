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

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.getInteger;
import static java.lang.Long.getLong;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.getProperty;
import static java.nio.charset.Charset.forName;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.transaction.Status.*;

/**
 * Collection of 'runtime' constants and low level tuning options used by the nio journal implementation.
 * <p/>
 * Note: The tuning options contained in this interface are meant for usage by experts only. If one of the
 * options turns out to be useful for the day to day configuration it will be moved into the main Configuration
 * instance.
 *
 * @author juergen kellerer, 2011-04-30
 */
public interface NioJournalConstants {

    /**
     * The amount of milliseconds for one hour.
     */
    long MS_PER_HOUR = 60 * 60 * 1000L;

    /**
     * The amount of milliseconds for one day.
     */
    long MS_PER_DAY = 24 * MS_PER_HOUR;

    /**
     * Is the charset used for unique names and for human readable content in the headers.
     * Important: must be a charset of constant size, using 8 bit encoding and be able to cover US-ASCII.
     */
    Charset NAME_CHARSET = forName("ISO-8859-1");

    // ---- SNIPPET-START: NioJournalTuningOptions

    /**
     * Max time to delay writes if force is not requested and queues have remaining capacity.
     * <p/>
     * Lowers the overall IO operations by combining write requests. This improves the overall
     * system performance in medium load situations as more resources remain available to other
     * services.
     */
    long WRITE_DELAY = getLong("bitronix.nio.journal.write.delay", SECONDS.toMillis(2));

    /**
     * Number of iterations that the write thread attempts to process more pending entries before it
     * forces the changes to disk (and releases waiting threads).
     * <p/>
     * Similar as write delay tries to reduce disk IO by combining individual writes, this value
     * attempts to reduce calls to force (fsync) by repeatedly writing any queued requests before
     * actually performing a requested force. Once forced any waiting threads are released.
     */
    int WRITE_ITERATIONS_BEFORE_FORCE = getInteger("bitronix.nio.journal.writes.before.force", 10);

    /**
     * Specifies the amount of slots (buffers, lock-free queue entries) to prepare for threads
     * trying to log a transaction.
     */
    int CONCURRENCY = getInteger("bitronix.nio.journal.concurrency", 1024);

    /**
     * Specifies the size of byte buffers to allocate for transaction serialization.
     * (should be as large as the majority of transactions may become)
     */
    int PRE_ALLOCATED_BUFFER_SIZE = getInteger("bitronix.nio.journal.buffer.size", 386);

    /**
     * Hard limit, defining the maximum time that a transaction may be held in the journal.
     * (= the maximum supported timeout for a transaction, defaults to 14 days)
     * <p/>
     * This value cleans journal records that are beyond this maximum age and is meant primarily
     * to protect the system from leakage that may be caused by crashes, hardware failures or software bugs.
     */
    long TRANSACTION_MAX_LIFETIME = max(MS_PER_DAY, getLong("bitronix.nio.journal.max.transaction.lifetime", MS_PER_DAY * 14));

    /**
     * Defines hard limit for journal record sizes to protect from OOM-Errors through DOS attacks
     * or failures with bit-flips inside the stored record size value (defaults to 64k).
     * <p/>
     * Note: Up to {@link #CONCURRENCY} * 2 * {@code JOURNAL_MAX_RECORD_SIZE} bytes of heap and direct buffer memory (half/half)
     * may be required at a time in the worst case scenario.
     */
    int JOURNAL_MAX_RECORD_SIZE = max(1024, getInteger("bitronix.nio.journal.max.record.size", 64 * 1024));

    /**
     * Is the offset of the min required free space in the journal before it is grown after a rollover.
     * <p/>
     * With the default value of 0.75, a grow happens if 3m are free after a rollover of a 4m file.
     * <p/>
     * Valid values are within a range of 0.1 <= x <= 0.9.
     * <p/>
     * If growing isn't possible, the system does not stop working but it logs a warning with every
     * failed attempt to grow the journal. For the case that more transactions are actually open
     * than fitting into the journal file, it will either grow or writes are blocked until transactions
     * get closed or fall into a timeout.
     */
    double JOURNAL_GROW_OFFSET = max(0.1D, min(0.9D, parseDouble(getProperty("bitronix.nio.journal.grow.offset", "0.75"))));

    /**
     * Is the new size of the journal when it is grown (relative to the journal size).
     * <p/>
     * With the default value of 1.5, a 4m file is grown by 4m * 1.5 => 6m.
     * <p/>
     * Settings this value to 1 disables the ability that the journal may grow.
     */
    double JOURNAL_GROW_RATIO = max(1D, parseDouble(getProperty("bitronix.nio.journal.grow.ratio", "1.5")));

    // ---- SNIPPET-END: NioJournalTuningOptions

    /**
     * Is a list of short human readable strings that map TX status IDs.
     */
    List<String> TRANSACTION_STATUS_STRINGS = unmodifiableList(asList(
            "0-ACT:", // Status.STATUS_ACTIVE
            "1-MRB:", // Status.STATUS_MARKED_ROLLBACK
            "2-PRE:", // Status.STATUS_PREPARED
            "3-CTD:", // Status.STATUS_COMMITTED
            "4-RBA:", // Status.STATUS_ROLLEDBACK
            "5-UKN:", // Status.STATUS_UNKNOWN
            "6-NTX:", // Status.STATUS_NO_TRANSACTION
            "7-PRI:", // Status.STATUS_PREPARING
            "8-COM:", // Status.STATUS_COMMITTING
            "9-ROL:", // Status.STATUS_ROLLINGBACK
            "<unkn>"  // out of bounds
    ));

    /**
     * Is a list of human readable strings that map TX status IDs.
     */
    List<String> TRANSACTION_LONG_STATUS_STRINGS = unmodifiableList(asList(
            "0-ACTIVE",            // Status.STATUS_ACTIVE
            "1-MARKED_ROLLBACK",   // Status.STATUS_MARKED_ROLLBACK
            "2-PREPARED",          // Status.STATUS_PREPARED
            "3-COMMITTED",         // Status.STATUS_COMMITTED
            "4-ROLLEDBACK",        // Status.STATUS_ROLLEDBACK
            "5-UNKNOWN",           // Status.STATUS_UNKNOWN
            "6-NO_TRANSACTION",    // Status.STATUS_NO_TRANSACTION
            "7-PREPARING",         // Status.STATUS_PREPARING
            "8-COMMITTING",        // Status.STATUS_COMMITTING
            "9-ROLLINGBACK",       // Status.STATUS_ROLLING_BACK
            "<unknown-status>"      // out of bounds
    ));

    /**
     * Collects the TX states that are used to finalize a transaction.
     */
    Set<Integer> FINAL_STATUS = unmodifiableSet(new HashSet<Integer>(asList(
            STATUS_COMMITTED,
            STATUS_ROLLEDBACK)));

    /**
     * Collects the TX states that are tracked in memory.
     */
    Set<Integer> TRACKED_STATUS = unmodifiableSet(new HashSet<Integer>(asList(
            STATUS_ACTIVE,
            STATUS_PREPARED,
            STATUS_COMMITTED,
            STATUS_ROLLEDBACK,
            STATUS_PREPARING,
            STATUS_COMMITTING,
            STATUS_ROLLING_BACK
    )));
}
