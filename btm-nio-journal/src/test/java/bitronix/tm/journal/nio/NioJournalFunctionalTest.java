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
import org.junit.Test;

import javax.transaction.Status;
import java.io.IOException;
import java.util.Arrays;
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
    protected JournalRecord getLogRecord(int status, int recordLength, int headerLength,
                                         long time, int sequenceNumber, int crc32, Uid gtrid,
                                         Set uniqueNames, int endRecord) {
        return new NioJournalRecord(status, recordLength, time, sequenceNumber, gtrid, uniqueNames, true);
    }

    @Override
    protected Journal getJournal() {
        return new NioJournal();
    }

    @Before
    public void setUp() throws Exception {
        NioJournal.getJournalFilePath().delete();
    }

    @Test
    public void testFSYNCCanBeSetFromCentralConfiguration() throws Exception {
        TransactionManagerServices.getConfiguration().setForcedWriteEnabled(true);
        assertFalse(new NioJournal().isSkipForce());
        TransactionManagerServices.getConfiguration().setForcedWriteEnabled(false);
        assertTrue(new NioJournal().isSkipForce());
    }

    @Test
    public void testJournalIsPositionedCorrectlyAfterOpen() throws Exception {
        int[] tests = {1, 2, 233, 4493};
        for (int test : tests) {
            setUp();
            doTestPositionIsCorrect(test);
            shutdownJournal();
        }
    }

    private void doTestPositionIsCorrect(int iterations) throws IOException {
        Uid gtrid = UidGenerator.generateUid();
        HashSet<String> uniqueNames = new HashSet<String>(Arrays.asList("1"));
        int rawRecordSize = NioJournalFileRecord.RECORD_HEADER_SIZE + NioJournalFileRecord.RECORD_TRAILER_SIZE +
                new NioJournalRecord(Status.STATUS_ACTIVE, gtrid, uniqueNames).getRecordLength();

        journal.open();
        for (int i = 0; i < iterations; i++)
            journal.log(Status.STATUS_ACTIVE, gtrid, uniqueNames);

        journal.close();
        assertFalse(((NioJournal) journal).isOpen());

        journal.open();
        assertEquals("Iterations:" + iterations,
                NioJournalFile.FIXED_HEADER_SIZE + (iterations * rawRecordSize),
                ((NioJournal) journal).journalFile.getPosition());
    }
}
