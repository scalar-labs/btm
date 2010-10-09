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
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * <p></p>
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
