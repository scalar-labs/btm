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
import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.TransactionLogAppender;
import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.utils.Uid;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.Status;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * Classic journal specific functional tests.
 *
 * @author lorban
 */
public class DiskJournalFunctionalTest extends AbstractJournalFunctionalTest {
    @Override
    protected TransactionLogRecord getLogRecord(int status, int recordLength, int headerLength, long time, int sequenceNumber, int crc32,
                                                Uid gtrid, Set uniqueNames, int endRecord) {
        return new TransactionLogRecord(status, recordLength, headerLength, time, sequenceNumber, crc32, gtrid, uniqueNames, endRecord);
    }

    @Override
    protected Journal getJournal() {
        return new DiskJournal();
    }

    @Before
    public void setUp() throws Exception {
        new File(TransactionManagerServices.getConfiguration().getLogPart1Filename()).delete();
        new File(TransactionManagerServices.getConfiguration().getLogPart2Filename()).delete();
    }

    @Test
    public void testExceptions() throws Exception {
        try {
            journal.force();
            fail("expected IOException");
        } catch (IOException ex) {
            assertEquals("cannot force log writing, disk logger is not open", ex.getMessage());
        }
        try {
            journal.log(0, null, null);
            fail("expected IOException");
        } catch (IOException ex) {
            assertEquals("cannot write log, disk logger is not open", ex.getMessage());
        }
        try {
            journal.collectDanglingRecords();
            fail("expected IOException");
        } catch (IOException ex) {
            assertEquals("cannot collect dangling records, disk logger is not open", ex.getMessage());
        }
    }

    @Test
    public void testCrc32Value() throws Exception {
        Set names = new HashSet();
        names.add("ActiveMQ");
        names.add("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");

        String uidString = "626974726F6E697853657276657249440000011C31FD45510000955B";
        byte[] uidArray = new byte[uidString.length() / 2];
        for (int i = 0; i < uidString.length() / 2; i++) {
            String substr = uidString.substring(i * 2, i * 2 + 2);
            byte b = (byte) Integer.parseInt(substr, 16);

            uidArray[i] = b;
        }
        Uid uid = new Uid(uidArray);

        TransactionLogRecord tlr = getLogRecord(Status.STATUS_COMMITTED,
                116, 28, 1220609394845L, 38266, -1380478121, uid, names, TransactionLogAppender.END_RECORD);
        assertTrue(tlr.isCrc32Correct());

        names = new TreeSet();
        names.add("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
        names.add("ActiveMQ");

        tlr = getLogRecord(Status.STATUS_COMMITTED,
                116, 28, 1220609394845L, 38266, -1380478121, uid, names, TransactionLogAppender.END_RECORD);
        assertTrue(tlr.isCrc32Correct());

        // test that removing unique names refreshes CRC32, see BTM-44
        Set namesToRemove = new HashSet();
        namesToRemove.add("ActiveMQ");

        tlr.removeUniqueNames(namesToRemove);

        assertTrue(tlr.isCrc32Correct());
    }
}
