package bitronix.tm.journal;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bitronix.tm.utils.UidGenerator;

/**
 * Source: http://bitronix-transaction-manager.10986.n7.nabble.com/Fix-for-BTM-138-td1701.html
 *
 * @author Kazuya Uno
 */
public class DiskJournalInterruptTest {

    private DiskJournal diskJournal = new DiskJournal();
    private Set<String> names = new HashSet<String>();

    @Before
    public void setUp() throws IOException {
        diskJournal.open();
    }

    @After
    public void tearDown() throws IOException {
        diskJournal.close();
    }

    @Test
    public void testShouldInterruptOnAThreadDontCauseOtherThreadToFail()
            throws Exception {
        // given: a thread writing logs
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    writeLog();
                } catch (IOException e) {
                    // normal
                }
            };
        };
        thread.start();

        // when thread is interrupted
        thread.interrupt();

        // this detect closed channel and reopen logs
        try {
            writeLog();
        } catch (ClosedChannelException cce) {
            // this is expected.
        }

        // then writing logs should work
        writeLog();

    }

    private void writeLog() throws IOException {
        diskJournal.log(Status.STATUS_COMMITTED, UidGenerator.generateUid(),
                names);
    }

}