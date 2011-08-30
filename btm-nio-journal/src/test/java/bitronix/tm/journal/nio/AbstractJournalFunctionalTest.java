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

import bitronix.tm.journal.JournalRecord;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import org.junit.Test;

import javax.transaction.Status;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

/**
 * Common functional test to be used with all journal implementations.
 *
 * @author lorban
 * @author juergen kellerer, 2011-04-30
 */
public abstract class AbstractJournalFunctionalTest extends AbstractJournalTest {

    protected abstract JournalRecord getLogRecord(int status, int recordLength, int headerLength, long time, int sequenceNumber, int crc32,
                                                  Uid gtrid, Set uniqueNames, int endRecord);

    @Test
    public void testSimpleCollectDanglingRecords() throws Exception {
        journal.open();
        Uid gtrid = UidGenerator.generateUid();

        assertEquals(0, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTING, gtrid, csvToSet("name1"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid, csvToSet("name1"));
        assertEquals(0, journal.collectDanglingRecords().size());
    }

    @Test
    public void testComplexCollectDanglingRecords() throws Exception {
        journal.open();
        Uid gtrid1 = UidGenerator.generateUid();
        Uid gtrid2 = UidGenerator.generateUid();

        assertEquals(0, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTING, gtrid1, csvToSet("name1,name2,name3"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid1, csvToSet("name1"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid1, csvToSet("name2"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid1, csvToSet("name3"));
        assertEquals(0, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTING, gtrid2, csvToSet("name1,name2,name3"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid2, csvToSet("name2"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid2, csvToSet("name3,name1"));
        assertEquals(0, journal.collectDanglingRecords().size());
    }

    @Test
    public void testCorruptedCollectDanglingRecords() throws Exception {
        journal.open();
        Uid gtrid1 = UidGenerator.generateUid();
        Uid gtrid2 = UidGenerator.generateUid();

        assertEquals(0, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTING, gtrid1, csvToSet("name1,name2,name3"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid1, csvToSet("name1"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid1, csvToSet("name3"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid1, csvToSet("name4"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid1, csvToSet("name2"));
        assertEquals(0, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid2, csvToSet("name1"));
        assertEquals(0, journal.collectDanglingRecords().size());
    }

    private SortedSet<String> csvToSet(String s) {
        SortedSet<String> result = new TreeSet<String>();
        String[] names = s.split("\\,");
        Collections.addAll(result, names);
        return result;
    }
}
