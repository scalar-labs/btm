package bitronix.tm.journal;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.util.*;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;

import javax.transaction.Status;

/**
 * Created by IntelliJ IDEA.
 * User: lorban
 * Date: 5 avr. 2008
 * Time: 18:09:46
 * To change this template use File | Settings | File Templates.
 */
public class DiskJournalTest extends TestCase {

    protected void setUp() throws Exception {
        new File(TransactionManagerServices.getConfiguration().getLogPart1Filename()).delete();
        new File(TransactionManagerServices.getConfiguration().getLogPart2Filename()).delete();
    }

    public void testExceptions() throws Exception {
        DiskJournal journal = new DiskJournal();

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

        journal.close();
    }

    public void testSimpleCollectDanglingRecords() throws Exception {
        DiskJournal journal = new DiskJournal();
        journal.open();
        Uid gtrid = UidGenerator.generateUid();

        assertEquals(0, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTING, gtrid, csvToSet("name1"));
        assertEquals(1, journal.collectDanglingRecords().size());

        journal.log(Status.STATUS_COMMITTED, gtrid, csvToSet("name1"));
        assertEquals(0, journal.collectDanglingRecords().size());


        journal.close();
    }

    public void testComplexCollectDanglingRecords() throws Exception {
        DiskJournal journal = new DiskJournal();
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

        journal.close();
    }

    public void testCorruptedCollectDanglingRecords() throws Exception {
        DiskJournal journal = new DiskJournal();
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

        journal.close();
    }

    public void testCrc32Value() throws Exception {
        SortedSet names = new TreeSet();
        names.add("ActiveMQ");
        names.add("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");

        String uidString = "626974726F6E697853657276657249440000011C31FD45510000955B";
        byte[] uidArray = new byte[uidString.length()/2];
        for (int i=0; i<uidString.length()/2 ;i++) {
            String substr = uidString.substring(i*2, i*2+2);
            byte b = (byte)Integer.parseInt(substr, 16);

            uidArray[i] = b;
        }
        Uid uid = new Uid(uidArray);

        TransactionLogRecord tlr = new TransactionLogRecord(Status.STATUS_COMMITTED, 116, 28, 1220609394845L, 38266, -1380478121, uid, names, TransactionLogAppender.END_RECORD);
        assertTrue(tlr.isCrc32Correct());

        names = new TreeSet();
        names.add("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
        names.add("ActiveMQ");

        tlr = new TransactionLogRecord(Status.STATUS_COMMITTED, 116, 28, 1220609394845L, 38266, -1380478121, uid, names, TransactionLogAppender.END_RECORD);
        assertTrue(tlr.isCrc32Correct());
    }

    private SortedSet csvToSet(String s) {
        SortedSet result = new TreeSet();
        String[] names = s.split("\\,");
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            result.add(name);
        }
        return result;
    }

}
