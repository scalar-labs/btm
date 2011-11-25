/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
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
import java.util.Set;

/**
 *
 * @author lorban
 */
public class MockJournal implements Journal {

    private Map<Uid, TransactionLogRecord> danglingRecords;

    private EventRecorder getEventRecorder() {
        return EventRecorder.getEventRecorder(this);
    }

    public void log(int status, Uid gtrid, Set<String> uniqueNames) throws IOException {
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
        danglingRecords = new HashMap<Uid, TransactionLogRecord>();
    }

    public void close() throws IOException {
        danglingRecords = null;
    }

    public void force() throws IOException {
    }

    public Map<Uid, TransactionLogRecord> collectDanglingRecords() throws IOException {
        return danglingRecords;
    }

    public void shutdown() {
    }
}
