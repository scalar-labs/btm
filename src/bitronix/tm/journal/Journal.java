package bitronix.tm.journal;

import bitronix.tm.internal.Uid;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Transaction logs journal implementations must implement this interface to provide functionality required by the
 * transaction manager.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public interface Journal {

    /**
     * Log a new transaction status to journal. Note that the journal will not check the flow of the transactions.
     * If you call this method with erroneous data, it will be added to the journal as-is.
     * @param status transaction status to log.
     * @param gtrid GTRID of the transaction.
     * @param uniqueNames unique names of the RecoverableXAResourceProducers participating in the transaction.
     * @throws IOException
     */
    public void log(int status, Uid gtrid, Set uniqueNames) throws IOException;

    /**
     * Open the journal. Integrity should be checked and an exception should be thrown in case the journal is corrupt.
     * @throws IOException
     */
    public void open() throws IOException;

    /**
     * Close this journal and release all underlying resources.
     * @throws IOException
     */
     public void close() throws IOException;

    /**
     * Force journal to synchronize with permanent storage.
     * @throws IOException
     */
    public void force() throws IOException;

    /**
     * Collect all dangling records of the journal, ie: COMMITTING records with no corresponding COMMITTED record.
     * @return a Map using Uid objects GTRID as key and {@link TransactionLogRecord} as value
     * @throws IOException
     */
    public Map collectDanglingRecords() throws IOException;

}
