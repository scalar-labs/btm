package bitronix.tm.mock.resource;

import bitronix.tm.utils.Uid;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.JournalLogEvent;

import javax.transaction.Status;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockJournal implements Journal {

    private Map danglingRecords = new HashMap();

    private EventRecorder getEventRecorder() {
        return EventRecorder.getEventRecorder(this);
    }

    public void log(int status, Uid gtrid, SortedSet uniqueNames) throws IOException {
        TransactionLogRecord record = new TransactionLogRecord(status, gtrid, uniqueNames);
        if (status == Status.STATUS_COMMITTING) {
            danglingRecords.put(gtrid, record);
        }
        if (status == Status.STATUS_COMMITTED) {
            danglingRecords.remove(gtrid);
        }
        getEventRecorder().addEvent(new JournalLogEvent(this, status, gtrid, uniqueNames));
    }

    public void open() throws IOException {
        danglingRecords = new HashMap();
    }

    public void close() throws IOException {
        danglingRecords = new HashMap();
    }

    public void force() throws IOException {
    }

    public Map collectDanglingRecords() throws IOException {
        return danglingRecords;
    }

    public void shutdown() {
    }
}
