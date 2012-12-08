/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.journal;

import bitronix.tm.utils.Service;
import bitronix.tm.utils.Uid;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Transaction logs journal implementations must implement this interface to provide functionality required by the
 * transaction manager.
 *
 * @author Ludovic Orban
 */
public interface Journal extends Service {

    /**
     * Log a new transaction status to journal. Note that the journal will not check the flow of the transactions.
     * If you call this method with erroneous data, it will be added to the journal as-is.
     *
     * @param status transaction status to log.
     * @param gtrid GTRID of the transaction.
     * @param uniqueNames unique names of the RecoverableXAResourceProducers participating in the transaction.
     * @throws IOException if an I/O error occurs.
     */
    public void log(int status, Uid gtrid, Set<String> uniqueNames) throws IOException;

    /**
     * Open the journal. Integrity should be checked and an exception should be thrown in case the journal is corrupt.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void open() throws IOException;

    /**
     * Close this journal and release all underlying resources.
     *
     * @throws IOException if an I/O error occurs.
     */
     public void close() throws IOException;

    /**
     * Force journal to synchronize with permanent storage.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void force() throws IOException;

    /**
     * Collect all dangling records of the journal, ie: COMMITTING records with no corresponding COMMITTED record.
     *
     * @return a Map using Uid objects GTRID as key and implementations of {@link JournalRecord} as value.
     * @throws IOException if an I/O error occurs.
     */
    public Map<Uid, JournalRecord> collectDanglingRecords() throws IOException;
}
