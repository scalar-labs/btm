package bitronix.tm.gui;

import bitronix.tm.utils.Decoder;
import bitronix.tm.journal.TransactionLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelListener;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * <p></p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class RawTransactionTableModel extends TransactionTableModel {

    private List displayedRows;

    private final static Logger log = LoggerFactory.getLogger(RawTransactionTableModel.class);
    public static final int GTRID_COL = 7;

    public RawTransactionTableModel(File filename) {
        try {
            readFullTransactionLog(filename);
        } catch (Exception ex) {
            log.error("corrupted log file", ex);
        }
        displayedRows = new ArrayList(tLogs);
    }

    public int getColumnCount() {
        return 8;
    }

    public int getRowCount() {
        return displayedRows.size();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Class getColumnClass(int columnIndex) {
        return String.class;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        TransactionLogRecord tlog = (TransactionLogRecord) displayedRows.get(rowIndex);
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
        return (TransactionLogRecord) displayedRows.get(row);
    }

    public void filterByGtrid(String gtrid) {
        if (gtrid == null) {
            displayedRows = new ArrayList(tLogs);
        }
        else {
            List newDis = new ArrayList();
            for (int i = 0; i < displayedRows.size(); i++) {
                TransactionLogRecord transactionLogRecord = (TransactionLogRecord) displayedRows.get(i);
                if (transactionLogRecord.getGtrid().toString().equals(gtrid))
                    newDis.add(transactionLogRecord);
            }
            displayedRows = newDis;
        }
    }
}
