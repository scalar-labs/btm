package bitronix.tm.journal;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Set;
import java.util.HashSet;

import bitronix.tm.utils.Uid;

/**
 * Used to read {@link TransactionLogRecord} objects from a log file.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TransactionLogCursor {

    private final static Logger log = LoggerFactory.getLogger(TransactionLogCursor.class);

    private final RandomAccessFile randomAccessFile;
    private long endPosition;

    /**
     * Create a TransactionLogCursor that will read from the specified file.
     * This opens a new read-only file descriptor.
     * @param file the file to read logs from
     * @throws IOException if an I/O error occurs.
     */
    public TransactionLogCursor(File file) throws IOException {
        this.randomAccessFile = new RandomAccessFile(file, "r");
        this.randomAccessFile.seek(TransactionLogHeader.CURRENT_POSITION_HEADER);
        endPosition = this.randomAccessFile.readLong();
    }

    /**
     * Fetch the next TransactionLogRecord from log, recalculating the CRC and checking it against the stored one.
     * InvalidChecksumException is thrown if the check fails.
     * @return the TransactionLogRecord or null if the end of the log file has been reached
     * @throws IOException if an I/O error occurs.
     */
    public TransactionLogRecord readLog() throws IOException {
        return readLog(false);
    }

    /**
     * Fetch the next TransactionLogRecord from log.
     * @param skipCrcCheck if set to false, the method will thow an InvalidChecksumException if the CRC on disk does
     *        not match the recalculated one. Otherwise, the CRC is not recalculated nor checked agains the stored one.
     * @return the TransactionLogRecord or null if the end of the log file has been reached
     * @throws IOException if an I/O error occurs.
     */
    public TransactionLogRecord readLog(boolean skipCrcCheck) throws IOException {
        synchronized (randomAccessFile) {
            long currentPosition = randomAccessFile.getFilePointer();
            if (currentPosition >= endPosition) {
                if (log.isDebugEnabled()) log.debug("end of transaction log file reached at " + randomAccessFile.getFilePointer());
                return null;
            }

            int status = randomAccessFile.readInt();
            int recordLength = randomAccessFile.readInt();

            // check that log is in file bounds
            long savedPos = randomAccessFile.getFilePointer();
            randomAccessFile.skipBytes(recordLength - 4);
            if (randomAccessFile.getFilePointer() + 4 > endPosition) {
                randomAccessFile.skipBytes(4);
                throw new CorruptedTransactionLogException("corrupted log found at position " + currentPosition +
                        " (record terminator outside of file bounds: " + randomAccessFile.getFilePointer() + " of " +
                        endPosition + ", recordLength: " + recordLength + ")");
            }

            // check for log terminator
            int endCode = randomAccessFile.readInt();
            long endOfRecordPosition = randomAccessFile.getFilePointer();
            if (endCode != TransactionLogAppender.END_RECORD)
                throw new CorruptedTransactionLogException("corrupted log found at position " + currentPosition +
                        " (no record terminator found)");
            randomAccessFile.seek(savedPos);


            int headerLength = randomAccessFile.readInt();
            long time = randomAccessFile.readLong();
            int sequenceNumber = randomAccessFile.readInt();
            int crc32 = randomAccessFile.readInt();
            byte gtridSize = randomAccessFile.readByte();

            // check that GTRID is not too long
            if (4 + 8 + 4 + 4 + 1 + gtridSize > recordLength) {
                randomAccessFile.seek(endOfRecordPosition);
                throw new CorruptedTransactionLogException("corrupted log found at position " + currentPosition +
                        " (GTRID size too long)");
            }

            byte[] gtridArray = new byte[gtridSize];
            randomAccessFile.readFully(gtridArray);
            Uid gtrid = new Uid(gtridArray);
            int uniqueNamesCount = randomAccessFile.readInt();
            Set uniqueNames = new HashSet();
            int currentReadCount = 4 + 8 + 4 + 4 + 1 + gtridSize + 4;

            for (int i=0; i<uniqueNamesCount ;i++) {
                int length = randomAccessFile.readShort();

                // check that names aren't too long
                currentReadCount += 2 + length;
                if (currentReadCount > recordLength) {
                    randomAccessFile.seek(endOfRecordPosition);
                    throw new CorruptedTransactionLogException("corrupted log found at position " + currentPosition +
                            " (unique names too long, " + (i+1) + " out of " + uniqueNamesCount + ", length: " + length +
                            ", currentReadCount: " + currentReadCount + ", recordLength: " + recordLength + ")");
                }

                byte[] nameBytes = new byte[length];
                randomAccessFile.readFully(nameBytes);
                uniqueNames.add(new String(nameBytes, "US-ASCII"));
            }
            int cEndRecord = randomAccessFile.readInt();

            TransactionLogRecord tlog = new TransactionLogRecord(status, recordLength, headerLength, time, sequenceNumber, crc32, gtrid, uniqueNames, cEndRecord);

            // check that CRC is okay
            if (!skipCrcCheck && !tlog.isCrc32Correct()) {
                randomAccessFile.seek(endOfRecordPosition);
                throw new CorruptedTransactionLogException("corrupted log found at position " + currentPosition + "(invalid CRC, recorded: " + tlog.getCrc32() +
                        ", calculated: " + tlog.calculateCrc32() + ")");
            }

            return tlog;
        }
    }

    /**
     * Close the cursor and the underlying file
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        synchronized (randomAccessFile) {
            randomAccessFile.close();
        }
    }

}
