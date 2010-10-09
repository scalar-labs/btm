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
package bitronix.tm.gui;

import bitronix.tm.journal.TransactionLogRecord;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <p></p>
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
        new TransactionLogDialog(frame, tlog).setVisible(true);
    }
}
