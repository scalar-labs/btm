package bitronix.tm.journal;

import static bitronix.tm.journal.ByteBufferUtil.createByteBuffer;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InterruptibleLockedRandomAccessFileTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private File inputFile;

    @Before
    public void setUp() throws Exception {
        inputFile = folder.newFile("btmlog-test.log");
    }

    @Test
    public void testOpenClose() throws Exception {
        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");
        file.close();
    }

    @Test
    public void testLockedOpen() throws Exception {
        final RandomAccessFile firstFile = new RandomAccessFile(inputFile, "rw");
        final FileChannel fileChannel = firstFile.getChannel();
        final FileLock lock = fileChannel.tryLock();
        assertNotNull("null lock", lock);

        try {
            new InterruptibleLockedRandomAccessFile(inputFile, "rw");
            fail("should not open a locked file");
        } catch (OverlappingFileLockException expected) {
        } finally {
            lock.release();
            fileChannel.close();
            firstFile.close();
        }
    }

    @Test
    public void testReadAfterClose() throws Exception {
        final String data = "testdata";
        FileUtils.writeStringToFile(inputFile, data, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");
        file.close();

        try {
            readFile(file, 1);
            fail("should not read after close");
        } catch (final IllegalStateException expected) {
        }
    }

    @Test
    public void testWriteAfterClose() throws Exception {
        final String data = "testdata";
        FileUtils.writeStringToFile(inputFile, data, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");
        file.close();

        final ByteBuffer buffer = createByteBuffer("testdata");
        final long position = 0L;
        try {
            file.write(buffer, position);
            fail("should not write after close");
        } catch (final IllegalStateException expected) {
        }
    }

    @Test
    public void testForceAfterClose() throws Exception {
        final String data = "testdata";
        FileUtils.writeStringToFile(inputFile, data, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");
        file.close();

        try {
            final boolean metaData = true;
            file.force(metaData);
            fail("should not force after close");
        } catch (final IllegalStateException expected) {
        }
    }

    @Test
    public void testPositionAfterClose() throws Exception {
        final String data = "testdata";
        FileUtils.writeStringToFile(inputFile, data, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");
        file.close();

        try {
            file.position(1L);
            fail("should not position after close");
        } catch (final IllegalStateException expected) {
        }
    }

    @Test
    public void testRead() throws Exception {
        final String data = "testdata";
        FileUtils.writeStringToFile(inputFile, data, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");
        verifyRead(data, file);
        file.close();
    }

    @Test
    public void testReadTwice() throws Exception {
        final String data = "testdata";
        FileUtils.writeStringToFile(inputFile, data, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");

        verifyRead(data, file);
        file.position(0L);
        verifyRead(data, file);

        file.close();
    }

    @Test
    public void testReadAfterInterrupt() throws Exception {
        final String data = "testdataTESTDATA";
        FileUtils.writeStringToFile(inputFile, data, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");

        verifyRead(data, file);
        file.position(0L);
        interruptCurrentThread();
        try {
            verifyRead(data, file);
            fail("interrupt should close the FileChannel");
        } catch (final ClosedByInterruptException expected) {
        }

        clearInterruptedFlag();

        verifyRead(data, file);
        file.close();
    }

    private void verifyRead(final String expectedData,
            final InterruptibleLockedRandomAccessFile file) throws Exception {
        final int bytesToRead = expectedData.getBytes("UTF-8").length;
        final String readData = readFile(file, bytesToRead);

        assertThat(readData).isEqualTo(expectedData);
    }

    private String readFile(final InterruptibleLockedRandomAccessFile file,
            final int bytesToRead) throws IOException {
        final ByteBuffer inputBuffer = ByteBuffer.allocate(bytesToRead);
        file.read(inputBuffer);
        return toString(inputBuffer);
    }

    private String toString(ByteBuffer buffer)
            throws UnsupportedEncodingException {
        return new String(buffer.array(), "UTF-8");
    }

    @Test
    public void testWrite() throws Exception {
        final String data = "testdata";

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");

        final ByteBuffer outputBuffer = createByteBuffer(data);

        final long position = 0L;
        file.write(outputBuffer, position);

        file.close();

        verifyFileContent(inputFile, data);
    }

    @Test
    public void testWriteAndForce() throws Exception {
        final String data = "testdata";

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");

        final ByteBuffer outputBuffer = createByteBuffer(data);

        final long position = 0L;
        file.write(outputBuffer, position);
        file.force(true);

        file.close();

        verifyFileContent(inputFile, data);
    }

    private void verifyFileContent(final File file, final String expectedData)
            throws IOException {
        final String fileContent = FileUtils.readFileToString(inputFile,
                "UTF-8");
        assertEquals(expectedData, fileContent);
    }

    @Test
    public void testTwoWrites() throws Exception {
        final String dataOne = "testdata";
        final String dataTwo = "TESTDATA";

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");

        final ByteBuffer dataOneBuffer = createByteBuffer(dataOne);
        file.write(dataOneBuffer, 0L);
        file.write(createByteBuffer(dataTwo), dataOneBuffer.capacity());

        file.close();

        verifyFileContent(inputFile, dataOne + dataTwo);
    }

    @Test
    public void testWriteAfterInterrupt() throws Exception {
        final String dataOne = "testdata";
        final String dataTwo = "TESTDATA";

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");
        file.write(createByteBuffer(dataOne), 0L);

        interruptCurrentThread();

        try {
            file.write(createByteBuffer(dataTwo), dataOne.length());
        } catch (final ClosedByInterruptException expected) {
        }

        clearInterruptedFlag();

        final String dataThree = "__third__";
        file.write(createByteBuffer(dataThree), dataOne.length());

        file.close();

        verifyFileContent(inputFile, dataOne + dataThree);
    }

    @Test
    public void testForceAfterInterrupt() throws Exception {
        final String dataOne = "testdata";
        final String dataTwo = "TESTDATA";

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");
        file.write(createByteBuffer(dataOne), 0L);

        interruptCurrentThread();

        try {
            file.write(createByteBuffer(dataTwo), dataOne.length());
        } catch (final ClosedByInterruptException expected) {
        }

        clearInterruptedFlag();

        file.force(true);

        file.close();

        verifyFileContent(inputFile, dataOne);
    }

    @Test
    public void testFilePositionSetOnWriteInterrupt() throws Exception {
        final String dataOne = "testdata";
        FileUtils.writeStringToFile(inputFile, dataOne, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");

        final ByteBuffer outputBuffer = createByteBuffer(dataOne);

        long position = 2;
        file.position(position);

        interruptCurrentThread();
        try {
            file.write(outputBuffer, 1L);
            fail("writing a FileChannel should fail on an interrupted thread");
        } catch (ClosedChannelException expected) {
        }
        clearInterruptedFlag();

        final String readData = readFile(file, 5);
        file.close();

        assertEquals("read from file", "stdat", readData);
    }

    @Test
    public void testFilePositionSetOnReadInterrupt() throws Exception {
        final String dataOne = "testdata";
        FileUtils.writeStringToFile(inputFile, dataOne, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");

        readFile(file, 4);

        interruptCurrentThread();
        try {
            readFile(file, 2);
            fail("reading a FileChannel should fail on an interrupted thread");
        } catch (ClosedChannelException expected) {
        }
        clearInterruptedFlag();

        final String readData = readFile(file, 4);
        file.close();

        assertEquals("read from file", "data", readData);
    }

    @Test
    public void testCloseAfterInterrupt() throws Exception {
        final String data = "testdata";
        FileUtils.writeStringToFile(inputFile, data, "UTF-8");

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");

        interruptCurrentThread();
        try {
            readFile(file, 1);
            fail("should not read after interrupt");
        } catch (final ClosedChannelException expected) {
        }
        clearInterruptedFlag();

        file.close();
    }

    private void interruptCurrentThread() {
        Thread.currentThread().interrupt();
    }

    private void clearInterruptedFlag() {
        Thread.interrupted();
    }

}
