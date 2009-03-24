package bitronix.tm.journal;

import bitronix.tm.utils.Encoder;
import bitronix.tm.utils.UidGenerator;
import bitronix.tm.utils.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Representation of a transaction log record on disk.
 * <p>On-disk format has been implemented following Mike Spille's recommendations. Quoting him:</p>
 *
 * <i><p><code>[RECORD_TYPE :4] [RECORD_LEN :4] [HEADER_LEN :4] [System.currentTimeMillis :8] [Sequence number :4]
 * [Checksum :4] [Payload :X] [END_RECORD_INDICATOR :4]</code></p>
 * <p>Where [RECORD_TYPE] is a passed-in record type from the TM. [RECORD_LEN] is the overall record length
 * (sans [RECORD_TYPE and [RECORD_LEN]). [HEADER_LEN] is the length of the remainder of the header - important if you
 * want to support easy upgrades of your format. The remaining pieces are the rest of the header, and the payload. The
 * header at least should have [System.currentTimeMillis] and [Sequence number], with the [sequence number] coming from
 * some monotically increasing sequence generator unique to the process. The [checksum] is optional for the paranoid
 * among us. The time information can be very useful for profiling and tracking down problems in production, and in
 * conjunction with the sequence number it can give you precise ordering. This doesn't give you much in this solution,
 * but can be priceless if you ever move to a system with multiple dual log file pairs to lessen single-threading on a
 * single log file pair. Finally, I like having an [END_RECORD_INDICATOR] as an extra corruption detector device - I'm
 * a suspenders and belt kind of guy. Actually, the END_RECORD_INDICATOR and [RECORD_LEN] in conjunction are very
 * useful in development, as well, to catch programming mistakes in the log system early.</p></i>
 *
 * <p>Payload contains <code>[GTRID LENGTH :1] [GTRID :A] [UNIQUE NAMES COUNT :4] ([UNIQUE NAME LENGTH :2] [UNIQUE NAME :Y] ...)</code>
 * which makes a major difference with Mike's proposed format because here a record can vary in length: the GTRID size
 * is A bytes long (A being the GTRID length) and there can be X unique names that are Y characters long, Y being eventually
 * different for each name.</p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @see <a href="http://jroller.com/page/pyrasun?entry=xa_exposed_part_iii_the">XA Exposed, Part III: The Implementor's Notebook</a>
 * @author lorban
 */
public class TransactionLogRecord {

    private final static Logger log = LoggerFactory.getLogger(TransactionLogRecord.class);

    private int status;
    private int recordLength;
    private int headerLength;
    private long time;
    private int sequenceNumber;
    private int crc32;
    private Uid gtrid;
    private SortedSet uniqueNames;
    private int endRecord;

    /**
     * Use this constructor when restoring a log from the disk.
     * @param status record type
     * @param recordLength record length excluding status and recordLength
     * @param headerLength length of all fields except gtrid, uniqueNames and endRecord
     * @param time current time in milliseconds
     * @param sequenceNumber atomically generated sequence number during a JVM's lifespan
     * @param crc32 checksum of the full record
     * @param gtrid global transaction id
     * @param uniqueNames unique names of XA data sources used in this transaction
     * @param endRecord end of record marker
     */
    public TransactionLogRecord(int status, int recordLength, int headerLength, long time, int sequenceNumber, int crc32, Uid gtrid, Set uniqueNames, int endRecord) {
        this.status = status;
        this.recordLength = recordLength;
        this.headerLength = headerLength;
        this.time = time;
        this.sequenceNumber = sequenceNumber;
        this.crc32 = crc32;
        this.gtrid = gtrid;
        this.uniqueNames = new TreeSet(uniqueNames);
        this.endRecord = endRecord;
    }

    /**
     * Create a new transaction log ready to be stored.
     * @param status record type
     * @param gtrid global transaction id
     * @param uniqueNames unique names of XA data sources used in this transaction
     */
    public TransactionLogRecord(int status, Uid gtrid, Set uniqueNames) {
        this.status = status;
        time = System.currentTimeMillis();
        sequenceNumber = UidGenerator.getNextSequenceNumber();
        this.gtrid = gtrid;
        this.uniqueNames = new TreeSet(uniqueNames);
        endRecord = TransactionLogAppender.END_RECORD;

        refresh();
    }

    public int getStatus() {
        return status;
    }

    public int getRecordLength() {
        return recordLength;
    }

    public int getHeaderLength() {
        return headerLength;
    }

    public long getTime() {
        return time;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getCrc32() {
        return crc32;
    }

    public Uid getGtrid() {
        return gtrid;
    }

    public Set getUniqueNames() {
        return Collections.unmodifiableSortedSet(uniqueNames);
    }

    public int getEndRecord() {
        return endRecord;
    }


    public void removeUniqueNames(Collection namesToRemove) {
        uniqueNames.removeAll(namesToRemove);
        refresh();
    }

    /**
     * Recalculate and store the dynamic values of this record: {@link #getRecordLength()}, {@link #getRecordHeaderLength()}
     * and {@link #calculateCrc32()}. This method must be called each time after the set of contained unique names is updated.
     */
    private void refresh() {
        recordLength = calculateRecordLength(uniqueNames);
        headerLength = getRecordHeaderLength();
        crc32 = calculateCrc32();
    }

    /**
     * Recalculate the CRC32 value of this record (using {@link #calculateCrc32()}) and compare it with the stored value.
     * @return true if the recalculated value equals the stored one, false otherwise.
     */
    public boolean isCrc32Correct() {
        return calculateCrc32() == getCrc32();
    }

    /**
     * Calculate the CRC32 value of this record.
     * @return the CRC32 value of this record.
     */
    public int calculateCrc32() {
        CRC32 crc32 = new CRC32();
        crc32.update(Encoder.intToBytes(status));
        crc32.update(Encoder.intToBytes(recordLength));
        crc32.update(Encoder.intToBytes(headerLength));
        crc32.update(Encoder.longToBytes(time));
        crc32.update(Encoder.intToBytes(sequenceNumber));
        crc32.update(gtrid.getArray());
        crc32.update(Encoder.intToBytes(uniqueNames.size()));

        Iterator it = uniqueNames.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            crc32.update(Encoder.shortToBytes((short) name.length()));
            try {
                crc32.update(name.getBytes("US-ASCII"));
            } catch (UnsupportedEncodingException ex) {
                log.error("unable to convert unique name bytes to US-ASCII", ex);
            }
        }

        crc32.update(Encoder.intToBytes(endRecord));
        return (int) crc32.getValue();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(128);

        sb.append("a Bitronix TransactionLogRecord with ");
        sb.append("status="); sb.append(Decoder.decodeStatus(status)); sb.append(", ");
        sb.append("recordLength="); sb.append(recordLength); sb.append(", ");
        sb.append("headerLength="); sb.append(headerLength); sb.append(", ");
        sb.append("time="); sb.append(time); sb.append(", ");
        sb.append("sequenceNumber="); sb.append(sequenceNumber); sb.append(", ");
        sb.append("crc32="); sb.append(crc32); sb.append(", ");
        sb.append("gtrid="); sb.append(gtrid.toString()); sb.append(", ");
        sb.append("uniqueNames=");
        Iterator it = uniqueNames.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            sb.append(o);
            if (it.hasNext())
                sb.append(',');
        }

        return sb.toString();
    }


    /**
     * this is the total size on disk of a TransactionLog.
     * @return recordLength
     */
    int calculateTotalRecordSize() {
        return calculateRecordLength(uniqueNames) + 4 + 4; // + status + record length
    }

    /**
     * this is the value needed by field recordLength in the TransactionLog.
     * @param uniqueNames the unique names ofthe record.
     * @return recordLength
     */
    private int calculateRecordLength(Set uniqueNames) {
        int totalSize = 0;

        Iterator it = uniqueNames.iterator();
        while (it.hasNext()) {
            String uniqueName = (String) it.next();
            totalSize += 2 + uniqueName.length(); // 2 bytes for storing the unique name length + unique name length
        }

        totalSize += getFixedRecordLength();

        return totalSize;
    }

    /**
     * Length of all the fixed size fields part of the record length header except status and record length.
     * @return fixedRecordLength
     */
    private int getFixedRecordLength() {
        return 4 + 8 + 4 + 4 + 1 + gtrid.getArray().length + 4 + 4; // record header length + current time + sequence number + checksum + GTRID size + GTRID + unique names count + end record marker
    }

    /**
     * Value needed by field headerLength in the TransactionLog.
     * @return headerLength
     */
    private int getRecordHeaderLength() {
        return 4 + 4 + 4 + 8 + 4 + 4; // status + record length + record header length + current time + sequence number + checksum
    }


}
