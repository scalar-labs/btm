package bitronix.tm.gui;

import bitronix.tm.internal.Decoder;
import bitronix.tm.internal.UidGenerator;
import bitronix.tm.journal.TransactionLogRecord;

import javax.swing.event.TableModelListener;
import java.io.File;
import java.util.Date;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class RawTransactionTableModel extends TransactionTableModel {

    private final static Logger log = LoggerFactory.getLogger(RawTransactionTableModel.class);

    public RawTransactionTableModel(File filename) {
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

    public boolean acceptLog(TransactionLogRecord tlog) {
        return true;
    }

    public TransactionLogRecord getRow(int row) {
        return (TransactionLogRecord) tLogs.get(row);
    }
}
