/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Juergen Kellerer.
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

package bitronix.tm.journal.nio;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.JournalRecord;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.transaction.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Nio journal specific functional tests.
 *
 * @author juergen kellerer, 2011-04-30
 */
public class NioJournalFunctionalTest extends AbstractJournalFunctionalTest {
    @Override
    protected JournalRecord getLogRecord(int status, int recordLength, int headerLength, long time, int sequenceNumber, int crc32,
                                         Uid gtrid, Set uniqueNames, int endRecord) {
        return new NioJournalRecord(status, recordLength, time, sequenceNumber, false, gtrid, new HashSet<String>(uniqueNames), true);
    }

    @Override
    protected Journal getJournal() {
        return new NioJournal();
    }

    @Before
    public void setUp() throws Exception {
        File file = NioJournal.getJournalFilePath();
        assertTrue(!file.isFile() || file.delete());
    }

    @Test
    public void testCannotOpenTheSameFileTwice() throws Exception {
        final File file = NioJournal.getJournalFilePath();
        journal.open();
        journal.open(); // is allowed as 2nd call to open(), re-opens the journal.

        // test accessing a opened journal in write mode.
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            try {
                outputStream.write(' ');
            } finally {
                outputStream.close();
            }
            fail("write should fail on opened journal.");
        } catch (IOException expected) {
        }

        journal.close();

        // test open fails if another process has the journal locked.
        RandomAccessFile rw = new RandomAccessFile(file, "rw");
        try {
            FileLock lock = rw.getChannel().lock();
            try {
                journal.open();
                fail("open should fail on locked file.");
            } catch (OverlappingFileLockException expected) {
            } finally {
                lock.release();
            }
        } finally {
            rw.close();
        }
    }

    @Test
    public void testExceptions() throws Exception {
        try {
            journal.force();
            fail("expected IOException");
        } catch (IOException ex) {
            assertEquals("The journal is not yet opened or was already closed.", ex.getMessage());
        }
        try {
            journal.log(0, null, null);
            fail("expected IOException");
        } catch (IOException ex) {
            assertEquals("The journal is not yet opened or was already closed.", ex.getMessage());
        }
        try {
            journal.collectDanglingRecords();
            fail("expected IOException");
        } catch (IOException ex) {
            assertEquals("The journal is not yet opened or was already closed.", ex.getMessage());
        }
    }

    @Test
    public void testFSYNCCanBeSetFromCentralConfiguration() throws Exception {
        TransactionManagerServices.getConfiguration().setForcedWriteEnabled(true);
        assertFalse(new NioJournal().isSkipForce());
        TransactionManagerServices.getConfiguration().setForcedWriteEnabled(false);
        assertTrue(new NioJournal().isSkipForce());
    }

    @Test
    public void testJournalGrowsIfOpenTransactionsExceedCapacity() throws Exception {
        HashSet<String> uniqueNames = new HashSet<String>(Arrays.asList("1"));
        int rawRecordSize = calculateRawRecordSize(UidGenerator.generateUid(), uniqueNames);

        journal.open();
        File journalFilePath = NioJournal.getJournalFilePath();
        long fileSize = journalFilePath.length();
        int iterations = (int) ((fileSize - NioJournalFile.FIXED_HEADER_SIZE) / rawRecordSize) + 1;

        HashSet<Uid> ids = new HashSet<Uid>(iterations);
        for (int i = 0; i < iterations; i++) {
            Uid id = UidGenerator.generateUid();
            journal.log(Status.STATUS_COMMITTING, id, uniqueNames);
            ids.add(id);
        }

        journal.close();

        assertTrue("journal was not grown after opening " + iterations + " transactions.", fileSize < journalFilePath.length());

        journal.open();
        assertEquals("not all transactions were stored.", ids, journal.collectDanglingRecords().keySet());
    }

    @Test
    public void testJournalIsPositionedCorrectlyAfterOpen() throws Exception {
        final Uid gtrid = UidGenerator.generateUid();
        final Set<String> uniqueNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("1")));
        final float rawRecordSize = calculateRawRecordSize(gtrid, uniqueNames), readBufferSize = NioJournalFileIterable.INITIAL_READ_BUFFER_SIZE;
        int[] recordCounts = {
                1,
                2,
                (int) Math.floor(readBufferSize / rawRecordSize) / 2,
                (int) Math.floor(readBufferSize / rawRecordSize) / 2 * 25,
                (int) Math.floor(readBufferSize / rawRecordSize),
                (int) Math.ceil(readBufferSize / rawRecordSize),
                (int) Math.floor(readBufferSize * 3 / rawRecordSize),
                (int) Math.ceil(readBufferSize * 3 / rawRecordSize),
                (int) Math.floor(readBufferSize * 25 / rawRecordSize),
                (int) Math.ceil(readBufferSize * 25 / rawRecordSize)};

        for (int records : recordCounts) {
            setUp();
            doTestPositionIsCorrect(records, gtrid, uniqueNames);
            shutdownJournal();
        }
    }

    private void doTestPositionIsCorrect(int iterations, Uid gtrid, Set<String> uniqueNames) throws IOException {
        int rawRecordSize = calculateRawRecordSize(gtrid, uniqueNames);

        journal.open();
        assertEquals(NioJournalFile.FIXED_HEADER_SIZE, ((NioJournal) journal).journalFile.getPosition());

        for (int i = 0; i < iterations; i++)
            journal.log(Status.STATUS_ACTIVE, gtrid, uniqueNames);

        journal.close();
        assertFalse(((NioJournal) journal).isOpen());

        journal.open();
        assertEquals("Iterations:" + iterations,
                NioJournalFile.FIXED_HEADER_SIZE + (iterations * rawRecordSize), ((NioJournal) journal).journalFile.getPosition());
    }

    private int calculateRawRecordSize(Uid gtrid, Set<String> uniqueNames) {
        return NioJournalFileRecord.RECORD_HEADER_SIZE + NioJournalFileRecord.RECORD_TRAILER_SIZE +
                new NioJournalRecord(Status.STATUS_ACTIVE, gtrid, uniqueNames).getRecordLength();
    }
}
