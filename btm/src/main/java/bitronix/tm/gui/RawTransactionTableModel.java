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

import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.utils.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class RawTransactionTableModel extends TransactionTableModel {

    private List<TransactionLogRecord> displayedRows;

    private final static Logger log = LoggerFactory.getLogger(RawTransactionTableModel.class);
    public static final int GTRID_COL = 7;

    public RawTransactionTableModel(File filename) {
        try {
            readFullTransactionLog(filename);
        } catch (Exception ex) {
            log.error("corrupted log file", ex);
        }
        displayedRows = new ArrayList<TransactionLogRecord>(tLogs);
    }

    @Override
    public int getColumnCount() {
        return 8;
    }

    @Override
    public int getRowCount() {
        return displayedRows.size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TransactionLogRecord tlog = displayedRows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return Decoder.decodeStatus(tlog.getStatus());
            case 1:
                return "" + tlog.getRecordLength();
            case 2:
                return "" + tlog.getHeaderLength();
            case 3:
                return Console.dateFormatter.format(new Date(tlog.getTime()));
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

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    @Override
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

    @Override
    public void addTableModelListener(TableModelListener l) {
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
    }

    @Override
    public boolean acceptLog(TransactionLogRecord tlog) {
        return true;
    }

    @Override
    public TransactionLogRecord getRow(int row) {
        return displayedRows.get(row);
    }

    public void filterByGtrid(String gtrid) {
        if (gtrid == null) {
            displayedRows = new ArrayList<TransactionLogRecord>(tLogs);
        }
        else {
            List<TransactionLogRecord> newDis = new ArrayList<TransactionLogRecord>();
            for (int i = 0; i < displayedRows.size(); i++) {
                TransactionLogRecord transactionLogRecord = displayedRows.get(i);
                if (transactionLogRecord.getGtrid().toString().equals(gtrid))
                    newDis.add(transactionLogRecord);
            }
            displayedRows = newDis;
        }
    }
}
