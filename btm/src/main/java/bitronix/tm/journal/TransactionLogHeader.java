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

import bitronix.tm.utils.Decoder;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Used to control a log file's header.
 * <p>The physical data is read when this object is created then cached. Calling setter methods sets the header field
 * then moves the file pointer back to the previous location.</p>
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


    private final FileChannel fc;
    private final long maxFileLength;

    private volatile int formatId;
    private volatile long timestamp;
    private volatile byte state;
    private volatile long position;

    /**
     * TransactionLogHeader are used to control headers of the specified RandomAccessFile.
     * All calls to setters are synchronized on the passed-in RandomAccessFile.
     * @param fc the file channel to read from.
     * @param maxFileLength the max file length.
     * @throws IOException if an I/O error occurs.
     */
    public TransactionLogHeader(FileChannel fc, long maxFileLength) throws IOException {
        this.fc = fc;
        this.maxFileLength = maxFileLength;

        synchronized (this.fc) {
            fc.position(FORMAT_ID_HEADER);
            ByteBuffer buf = ByteBuffer.allocate(4 + 8 + 1 + 8);
            while (buf.hasRemaining()) {
                this.fc.read(buf);
            }
            buf.flip();
            formatId = buf.getInt();
            timestamp = buf.getLong();
            state = buf.get();
            position = buf.getLong();
            fc.position(position);
        }

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
     * @throws IOException if an I/O error occurs.
     */
    public void setFormatId(int formatId) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putInt(formatId);
        buf.flip();
        synchronized (fc) {
            long currentPos = fc.position();
            fc.position(FORMAT_ID_HEADER);
            while (buf.hasRemaining()) {
                this.fc.write(buf);
            }
            fc.position(currentPos);
            this.formatId = formatId;
        }
    }

    /**
     * Set TIMESTAMP_HEADER.
     * @see #TIMESTAMP_HEADER
     * @param timestamp the TIMESTAMP_HEADER value.
     * @throws IOException if an I/O error occurs.
     */
    public void setTimestamp(long timestamp) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(position);
        buf.flip();
        synchronized (fc) {
            long currentPos = fc.position();
            fc.position(TIMESTAMP_HEADER);
            while (buf.hasRemaining()) {
                this.fc.write(buf);
            }
            fc.position(currentPos);
            this.timestamp = timestamp;
        }
    }

    /**
     * Set STATE_HEADER.
     * @see #STATE_HEADER
     * @param state the STATE_HEADER value.
     * @throws IOException if an I/O error occurs.
     */
    public void setState(byte state) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(state);
        buf.flip();
        synchronized (fc) {
            long currentPos = fc.position();
            fc.position(STATE_HEADER);
            while (buf.hasRemaining()) {
                this.fc.write(buf);
            }
            fc.position(currentPos);
            this.state = state;
        }
    }

    /**
     * Set CURRENT_POSITION_HEADER.
     * @see #CURRENT_POSITION_HEADER
     * @param position the CURRENT_POSITION_HEADER value.
     * @throws IOException if an I/O error occurs.
     */
    public void setPosition(long position) throws IOException {
        if (position < HEADER_LENGTH)
            throw new IOException("invalid position " + position + " (too low)");
        if (position >= maxFileLength)
            throw new IOException("invalid position " + position + " (too high)");

        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(position);
        buf.flip();
        synchronized (fc) {
            fc.position(CURRENT_POSITION_HEADER);
            while (buf.hasRemaining()) {
                this.fc.write(buf);
            }
            fc.position(position);
            this.position = position;
        }
    }

    /**
     * Advance CURRENT_POSITION_HEADER.
     * @see #setPosition
     * @param distance the value to add to the current position.
     * @throws IOException if an I/O error occurs.
     */
    public void goAhead(long distance) throws IOException {
        setPosition(getPosition() + distance);
    }

    /**
     * Rewind CURRENT_POSITION_HEADER back to the beginning of the file.
     * @see #setPosition
     * @throws IOException if an I/O error occurs.
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
