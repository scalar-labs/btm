package bitronix.tm.gui;

import bitronix.tm.journal.TransactionLogRecord;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <p></p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TransactionTableMouseListener extends MouseAdapter {

    private JFrame frame;
    private JTable table;

    public TransactionTableMouseListener(JFrame frame, JTable table) {
        this.frame = frame;
        this.table = table;
    }

    public void mouseClicked(MouseEvent evt) {
        if (evt.getClickCount() == 2) {
            showDetails();
        }
    }

    private void showDetails() {
        TransactionLogRecord tlog = ((TransactionTableModel)table.getModel()).getRow(table.getSelectedRow());
        new TransactionLogDialog(frame, tlog).show();
    }
}
