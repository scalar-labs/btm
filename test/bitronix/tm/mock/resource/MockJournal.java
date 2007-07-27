package bitronix.tm.mock.resource;

import bitronix.tm.internal.Uid;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.JournalLogEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockJournal implements Journal {

    private EventRecorder getEventRecorder() {
        return EventRecorder.getEventRecorder(this);
    }

    public void log(int status, Uid gtrid, Set jndiNames) throws IOException {
        getEventRecorder().addEvent(new JournalLogEvent(this, status, gtrid, jndiNames));
    }

    public void open() throws IOException {
    }

    public void close() throws IOException {
    }

    public void force() throws IOException {
    }

    public Map collectDanglingRecords() throws IOException {
        return new HashMap();
    }

    public void shutdown() {
    }
}
