package bitronix.tm.journal;

import bitronix.tm.utils.Uid;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * No-op journal. Do not use for anything else than testing as the transaction manager cannot guarantee
 * data integrity with this journal implementation.
 *
 * @author lorban
 */
public class NullJournal implements Journal {

    public NullJournal() {
    }

    public void log(int status, Uid gtrid, Set uniqueNames) throws IOException {
    }

    public void open() throws IOException {
    }

    public void close() throws IOException {
    }

    public void force() throws IOException {
    }

    public Map collectDanglingRecords() throws IOException {
        return Collections.EMPTY_MAP;
    }

    public void shutdown() {
    }

    public String toString() {
        return "a NullJournal";
    }
}
