package bitronix.tm.gui;

import bitronix.tm.journal.TransactionLogRecord;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * <p></p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TransactionTableCellRenderer extends DefaultTableCellRenderer {

    public TransactionTableCellRenderer() {
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TransactionLogRecord tlog = (TransactionLogRecord) ((RawTransactionTableModel)table.getModel()).getRow(row);
        if (!tlog.isCrc32Correct()) {
            if (isSelected)
                setBackground(table.getSelectionBackground());
            else
                component.setBackground(Color.RED);
        }
        else {
            if (isSelected)
                setBackground(table.getSelectionBackground());
            else
                component.setBackground(Color.WHITE);
        }
        return component;
    }

}
