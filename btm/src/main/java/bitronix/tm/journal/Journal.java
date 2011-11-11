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
package bitronix.tm.journal;

import bitronix.tm.utils.Uid;
import bitronix.tm.utils.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Transaction logs journal implementations must implement this interface to provide functionality required by the
 * transaction manager.
 *
 * @author lorban
 */
public interface Journal extends Service {

    /**
     * Log a new transaction status to journal. Note that the journal will not check the flow of the transactions.
     * If you call this method with erroneous data, it will be added to the journal as-is.
     * @param status transaction status to log.
     * @param gtrid GTRID of the transaction.
     * @param uniqueNames unique names of the RecoverableXAResourceProducers participating in the transaction.
     * @throws IOException if an I/O error occurs.
     */
    public void log(int status, Uid gtrid, Set<String> uniqueNames) throws IOException;

    /**
     * Open the journal. Integrity should be checked and an exception should be thrown in case the journal is corrupt.
     * @throws IOException if an I/O error occurs.
     */
    public void open() throws IOException;

    /**
     * Close this journal and release all underlying resources.
     * @throws IOException if an I/O error occurs.
     */
     public void close() throws IOException;

    /**
     * Force journal to synchronize with permanent storage.
     * @throws IOException if an I/O error occurs.
     */
    public void force() throws IOException;

    /**
     * Collect all dangling records of the journal, ie: COMMITTING records with no corresponding COMMITTED record.
     * @return a Map using Uid objects GTRID as key and {@link TransactionLogRecord} as value
     * @throws IOException if an I/O error occurs.
     */
    public Map<Uid, TransactionLogRecord> collectDanglingRecords() throws IOException;

}
