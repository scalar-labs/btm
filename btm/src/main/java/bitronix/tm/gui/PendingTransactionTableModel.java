/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.gui;

import bitronix.tm.journal.JournalRecord;
import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.utils.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelListener;
import javax.transaction.Status;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p></p>
 *
 * @author Ludovic Orban
 */
public class PendingTransactionTableModel extends TransactionTableModel {

    private final static Logger log = LoggerFactory.getLogger(PendingTransactionTableModel.class);

    public PendingTransactionTableModel(File filename) {
        try {
            readFullTransactionLog(filename);
        } catch (Exception ex) {
            log.error("corrupted log file", ex);
        }
    }

    public int getColumnCount() {
        return 8;
    }

    public int getRowCount() {
        return tLogs.size();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Class getColumnClass(int columnIndex) {
        return String.class;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        TransactionLogRecord tlog = (TransactionLogRecord) tLogs.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return Decoder.decodeStatus(tlog.getStatus());
            case 1:
                return "" + tlog.getRecordLength();
            case 2:
                return "" + tlog.getHeaderLength();
            case 3:
                return "" + tlog.getTime();
            case 4:
                return "" + tlog.getSequenceNumber();
            case 5:
                return "" + tlog.getCrc32();
            case 6:
                return "" + tlog.getUniqueNames().size();
            case 7:
                return tlog.getGtrid().toString();
            default:
                return null;
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Record Status";
            case 1:
                return "Record length";
            case 2:
                return "Header length";
            case 3:
                return "Record time";
            case 4:
                return "Record sequence number";
            case 5:
                return "CRC";
            case 6:
                return "Resources";
            case 7:
                return "GTRID";
            default:
                return null;
        }
    }

    public void addTableModelListener(TableModelListener l) {
    }

    public void removeTableModelListener(TableModelListener l) {
    }


    private Map pendingTLogs = new HashMap();

    protected void readFullTransactionLog(File filename) throws IOException {
        super.readFullTransactionLog(filename);
        pendingTLogs.clear();
    }

    public boolean acceptLog(JournalRecord tlog) {
        if (tlog.getStatus() == Status.STATUS_COMMITTING) {
            pendingTLogs.put(tlog.getGtrid(), tlog);
            return true;
        }
        if (tlog.getStatus() == Status.STATUS_COMMITTED  ||  tlog.getStatus() == Status.STATUS_ROLLEDBACK  &&  pendingTLogs.containsKey(tlog.getGtrid().toString())) {
            tLogs.remove(pendingTLogs.get(tlog.getGtrid()));
        }
        return false;
    }

    public TransactionLogRecord getRow(int row) {
        return (TransactionLogRecord) tLogs.get(row);
    }
}
