package bitronix.tm.journal;

import static bitronix.tm.journal.ByteBufferUtil.createByteBuffer;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InterruptibleLockedRandomAccessFileStressTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private File inputFile;

    private final InterruptableThreadList threadList = new InterruptableThreadList();

    private final InterruptService interruptService = new InterruptService(
            threadList);

    @Before
    public void setUp() throws Exception {
        inputFile = folder.newFile("bitronix-stresstest.log");
        interruptService.start();
    }

    @After
    public void tearDown() throws Exception {
        interruptService.stop();
    }

    @Test
    public void stressTestWriteInterrupts() throws Exception {
        final int recordLength = 15;
        final int taskNumber = 10000;
        initializeFileContent(recordLength, taskNumber);

        final InterruptibleLockedRandomAccessFile file = new InterruptibleLockedRandomAccessFile(
                inputFile, "rw");

        final ExecutorService executorService = Executors.newFixedThreadPool(4);

        final AtomicLong successfulWrites = new AtomicLong();
        final AtomicLong writeErrors = new AtomicLong();
        for (int i = 0; i < taskNumber; i++) {
            final int taskId = i;
            final String data = createRecord(taskId);
            assertThat(data.length()).isLessThanOrEqualTo(recordLength);
            executorService.submit(new Runnable() {

                @Override
                public void run() {
                    threadList.addCurrentThread();
                    try {
                        final int position = taskId * recordLength;
                        file.write(createByteBuffer(data), position);
                        successfulWrites.incrementAndGet();
                    } catch (final Exception expected) {
                        writeErrors.incrementAndGet();
                    } finally {
                        threadList.removeCurrentThread();
                    }
                }
            });
        }

        shutdownExecutor(executorService, 30, TimeUnit.SECONDS);

        file.close();

        assertThat(successfulWrites.get() + writeErrors.get()).isEqualTo(
                taskNumber);

        final long writtenRecords = countWrittenRecords(taskNumber);
        final long missingRecords = countMissingRecords(taskNumber);
        assertThat(writtenRecords + missingRecords).isEqualTo(taskNumber);

        // System.out.println("written: " + writtenRecords);
        // System.out.println("missing: " + missingRecords);
        // System.out.println("successful writes: " + successfulWrites);
        // System.out.println("write errors: " + writeErrors);
        // System.out.println("interrupts: "
        //         + interruptService.getSuccessfulInterrupts());

        assertThat(writtenRecords).isGreaterThanOrEqualTo(
                successfulWrites.get());
        assertThat(missingRecords).isLessThanOrEqualTo(writeErrors.get());
        assertThat(interruptService.getSuccessfulInterrupts())
                .isGreaterThanOrEqualTo(missingRecords);
    }

    private void initializeFileContent(final int recordLength,
            final int taskNumber) throws Exception {
        final String initialFileConent = StringUtils.repeat(".", recordLength
                * taskNumber);
        FileUtils.writeStringToFile(inputFile, initialFileConent);
    }

    private String createRecord(final int recordId) {
        return String.format("data%5dX", recordId);
    }

    private long countMissingRecords(final int taskNumber) throws Exception {
        final String writtenContent = FileUtils.readFileToString(inputFile,
                "UTF-8");
        long missingRecords = 0;
        for (int taskId = 0; taskId < taskNumber; taskId++) {
            final String data = createRecord(taskId);
            if (!writtenContent.contains(data)) {
                missingRecords++;
            }
        }
        return missingRecords;
    }

    private long countWrittenRecords(final int taskNumber) throws Exception {
        final String writtenContent = FileUtils.readFileToString(inputFile,
                "UTF-8");
        long writtenRecords = 0;
        for (int taskId = 0; taskId < taskNumber; taskId++) {
            final String data = createRecord(taskId);
            if (writtenContent.contains(data)) {
                writtenRecords++;
            }
        }
        return writtenRecords;
    }

    private void shutdownExecutor(final ExecutorService executorService,
            final long timeout, final TimeUnit timeoutUnit)
            throws InterruptedException {
        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(timeout,
                timeoutUnit);
        assertTrue("termination", terminated);
    }
}
