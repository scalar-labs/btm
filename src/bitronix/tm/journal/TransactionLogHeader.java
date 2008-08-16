package bitronix.tm.journal;

import bitronix.tm.utils.Decoder;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Used to control a log file's header.
 * <p>The physical data is read when this object is created then cached. Calling setter methods sets the header field
 * then moves the file pointer back to the previous location.</p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TransactionLogHeader {

    private final static Logger log = LoggerFactory.getLogger(TransactionLogHeader.class);

    /**
     * Position of the format ID in the header (see {@link bitronix.tm.BitronixXid#FORMAT_ID}).
     */
    public final static int FORMAT_ID_HEADER = 0;

    /**
     * Position of the timestamp in the header.
     */
    public final static int TIMESTAMP_HEADER = FORMAT_ID_HEADER + 4;

    /**
     * Position of the log file state in the header.
     */
    public final static int STATE_HEADER = TIMESTAMP_HEADER + 8;

    /**
     * Position of the current log position in the header.
     */
    public final static int CURRENT_POSITION_HEADER = STATE_HEADER + 1;

    /**
     * Total length of the header.
     */
    public final static int HEADER_LENGTH = CURRENT_POSITION_HEADER + 8;

    /**
     * State of the log file when it has been closed properly.
     */
    public final static byte CLEAN_LOG_STATE = 0;

    /**
     * State of the log file when it hasn't been closed properly or it is still open.
     */
    public final static byte UNCLEAN_LOG_STATE = -1;


    private RandomAccessFile randomAccessFile;
    private int formatId;
    private long timestamp;
    private byte state;
    private long position;
    private long maxFileLength;

    /**
     * TransactionLogHeader are used to control headers of the specified RandomAccessFile.
     * All calls to setters are synchronized on the passed-in RandomAccessFile.
     * @param randomAccessFile
     * @param maxFileLength
     * @throws IOException
     */
    public TransactionLogHeader(RandomAccessFile randomAccessFile, long maxFileLength) throws IOException {
        this.randomAccessFile = randomAccessFile;
        this.maxFileLength = maxFileLength;

        randomAccessFile.seek(FORMAT_ID_HEADER);
        formatId = randomAccessFile.readInt();
        timestamp = randomAccessFile.readLong();
        state = randomAccessFile.readByte();
        position = randomAccessFile.readLong();
        randomAccessFile.seek(position);

        if (log.isDebugEnabled()) log.debug("read header " + this);
    }

    /**
     * Get FORMAT_ID_HEADER.
     * @see #FORMAT_ID_HEADER
     * @return the FORMAT_ID_HEADER value.
     */
    public int getFormatId() {
        return formatId;
    }

    /**
     * Get TIMESTAMP_HEADER.
     * @see #TIMESTAMP_HEADER
     * @return the TIMESTAMP_HEADER value.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get STATE_HEADER.
     * @see #STATE_HEADER
     * @return the STATE_HEADER value.
     */
    public byte getState() {
        return state;
    }

    /**
     * Get CURRENT_POSITION_HEADER.
     * @see #CURRENT_POSITION_HEADER
     * @return the CURRENT_POSITION_HEADER value.
     */
    public long getPosition() {
        return position;
    }

    /**
     * Set FORMAT_ID_HEADER.
     * @see #FORMAT_ID_HEADER
     * @param formatId the FORMAT_ID_HEADER value.
     * @throws IOException
     */
    public void setFormatId(int formatId) throws IOException {
        synchronized (randomAccessFile) {
            long currentPos = randomAccessFile.getFilePointer();
            randomAccessFile.seek(FORMAT_ID_HEADER);
            randomAccessFile.writeInt(formatId);
            randomAccessFile.seek(currentPos);
        }

        this.formatId = formatId;
    }

    /**
     * Set TIMESTAMP_HEADER.
     * @see #TIMESTAMP_HEADER
     * @param timestamp the TIMESTAMP_HEADER value.
     * @throws IOException
     */
    public void setTimestamp(long timestamp) throws IOException {
        synchronized (randomAccessFile) {
            long currentPos = randomAccessFile.getFilePointer();
            randomAccessFile.seek(TIMESTAMP_HEADER);
            randomAccessFile.writeLong(timestamp);
            randomAccessFile.seek(currentPos);
        }

        this.timestamp = timestamp;
    }

    /**
     * Set STATE_HEADER.
     * @see #STATE_HEADER
     * @param state the STATE_HEADER value.
     * @throws IOException
     */
    public void setState(byte state) throws IOException {
        synchronized (randomAccessFile) {
            long currentPos = randomAccessFile.getFilePointer();
            randomAccessFile.seek(STATE_HEADER);
            randomAccessFile.writeByte(state);
            randomAccessFile.seek(currentPos);
        }

        this.state = state;
    }

    /**
     * Set CURRENT_POSITION_HEADER.
     * @see #CURRENT_POSITION_HEADER
     * @param position the CURRENT_POSITION_HEADER value.
     * @throws IOException
     */
    public void setPosition(long position) throws IOException {
        if (position < HEADER_LENGTH)
            throw new IOException("invalid position " + position + " (too low)");
        if (position >= maxFileLength)
            throw new IOException("invalid position " + position + " (too high)");

        synchronized (randomAccessFile) {
            randomAccessFile.seek(CURRENT_POSITION_HEADER);
            randomAccessFile.writeLong(position);
            randomAccessFile.seek(position);
        }

        this.position = position;
    }

    /**
     * Advance CURRENT_POSITION_HEADER.
     * @see #setPosition
     * @param distance the value to add to the current position.
     * @throws IOException
     */
    public void goAhead(long distance) throws IOException {
        setPosition(getPosition() + distance);
    }

    /**
     * Rewind CURRENT_POSITION_HEADER back to the beginning of the file.
     * @see #setPosition
     * @throws IOException
     */
    public void rewind() throws IOException {
        setPosition(HEADER_LENGTH);
    }

    /**
     * Create human-readable String representation.
     * @return a human-readable String representing this object's state.
     */
    public String toString() {
        return "a Bitronix TransactionLogHeader with timestamp=" + timestamp +
                ", state=" + Decoder.decodeHeaderState(state) +
                ", position=" + position;
    }

}
