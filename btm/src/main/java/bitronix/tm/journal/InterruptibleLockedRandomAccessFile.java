package bitronix.tm.journal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class InterruptibleLockedRandomAccessFile {

    private final File file;
    private final String mode;
    private RandomAccessFile openedFile;
    private FileChannel fileChannel;
    private FileLock fileLock;
    private long currentPosition = 0;
    private boolean closed;

    public InterruptibleLockedRandomAccessFile(final File file, final String mode)
            throws IOException {
        this.file = file;
        this.mode = mode;
        open();
    }

    private synchronized void open() throws IOException {
        openedFile = new RandomAccessFile(file, mode);
        fileChannel = openedFile.getChannel();

        final boolean shared = false;
        this.fileLock = fileChannel
                .tryLock(0, TransactionLogHeader.TIMESTAMP_HEADER, shared);
        if (this.fileLock == null) {
            throw new IOException("File " + file.getAbsolutePath()
                    + " is locked. Is another instance already running?");
        }
    }

    public synchronized final void close() throws IOException {
        try {
            if (!fileLock.isValid()) {
                checkState(!fileChannel.isOpen(), "invalid/unhandled state");
                return;
            }
            fileLock.release();
            fileChannel.close();
            openedFile.close();
        } finally {
            closed = true;
        }
    }

    public synchronized void position(final long newPosition) throws IOException {
        checkNotClosed();
        reopenFileChannelIfClosed();

        fileChannel.position(newPosition);
        currentPosition = newPosition;
    }

    private void checkNotClosed() {
        checkState(!closed, "File has been closed");
    }

    private static void checkState(final boolean expression, final String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public synchronized void force(final boolean metaData) throws IOException {
        checkNotClosed();
        reopenFileChannelIfClosed();

        fileChannel.force(metaData);
    }

    public synchronized int write(final ByteBuffer src, final long position)
            throws IOException {
        checkNotClosed();
        reopenFileChannelIfClosed();

        return fileChannel.write(src, position);
    }

    public synchronized void read(final ByteBuffer buffer) throws IOException {
        checkNotClosed();
        reopenFileChannelIfClosed();

        fileChannel.read(buffer);
        currentPosition = fileChannel.position();
    }

    private void reopenFileChannelIfClosed() throws IOException {
        if (!fileChannel.isOpen()) {
            open();
        }

        if (fileChannel.position() != currentPosition) {
            fileChannel.position(currentPosition);
        }
    }
}