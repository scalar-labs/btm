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

/**
 * Collection of 'runtime' constants used by the nio journal implementation.
 *
 * @author juergen kellerer, 2011-04-30
 */
public interface NioJournalConstants {
    /**
     * Is the charset used for unique names and for human readable content in the headers.
     * Important: must be a charset of constant size, using 8 bit encoding and be able to cover US-ASCII.
     */
    Charset NAME_CHARSET = Charset.forName("ISO-8859-1");

    /**
     * Hard limit, defining the maximum time that a transaction may be held in the journal.
     * (= the maximum supported timeout for a transaction, defaults to 14 days)
     */
    long TRANSACTION_MAX_LIFETIME = Long.getLong(
            "bitronix.nio.journal.transaction.timeout", 14 * 24 * 60 * 60 * 1000L);

    /**
     * Is the offset of the min required free space in the journal before it is grown after a rollover.
     * <p/>
     * E.g. with 0.75, a grow happens if 3m are free after a rollover of a 4m file.
     */
    double JOURNAL_GROW_OFFSET = Double.parseDouble(System.getProperty(
            "bitronix.nio.journal.grow.offset", "0.75"));

    /**
     * Is the new size of the journal when it is grown (relative to the journal size).
     */
    double JOURNAL_GROW_RATIO = Double.parseDouble(System.getProperty(
            "bitronix.nio.journal.grow.ratio", "2"));

    /**
     * Number of iterations that the write thread attempts to process more pending entries
     * before it forces the changes to disk (and releases waiting threads).
     */
    int WRITE_ITERATIONS_BEFORE_FORCE = 10;

    /**
     * Specifies the amount of slots (buffers, lock-free queue entries) to prepare for threads
     * trying to log a transaction.
     */
    int CONCURRENCY = Integer.getInteger("bitronix.nio.journal.concurrency", 4 * 1024);

    /**
     * Specifies the size of byte buffers to allocate for transaction serialization.
     * (should be as large as the majority of transactions may become)
     */
    int PRE_ALLOCATED_BUFFER_SIZE = Integer.getInteger("bitronix.nio.journal.buffer.size", 386);
}
