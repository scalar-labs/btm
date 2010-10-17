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

import javax.swing.table.DefaultTableModel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p></p>
 *
 * @author lorban
 */
public class DuplicatedGtridTableModel extends DefaultTableModel {

    private Map duplicatedGtrids;

    public DuplicatedGtridTableModel(Map map) {
        super(map.size(), 2);
        this.duplicatedGtrids = map;
    }

    public String getColumnName(int column) {
        switch (column) {
            case 0: return "Sequence number";
            case 1: return "GTRID";
            default: return "?";
        }
    }

    public Object getValueAt(int row, int column) {
        Iterator it = duplicatedGtrids.entrySet().iterator();
        List tlogs = null;
        int i=0;
        while (i<=row && it.hasNext()) {
            i++;
            Map.Entry entry = (Map.Entry) it.next();
            tlogs = (List) entry.getValue();
        }

        if (tlogs == null)
            return null;

        switch (column) {
            case 0: return buildTlogsSequenceNumber(tlogs);
            case 1: return buildTlogsGtrid(tlogs);
            default: return "?";
        }
    }

    private String buildTlogsSequenceNumber(List tlogs) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < tlogs.size(); i++) {
            TransactionLogRecord tlog = (TransactionLogRecord) tlogs.get(i);
            sb.append(tlog.getSequenceNumber());
            if (i < tlogs.size() -1)
                sb.append(", ");
        }
        return sb.toString();
    }

    private String buildTlogsGtrid(List tlogs) {
        TransactionLogRecord tlog = (TransactionLogRecord) tlogs.get(0);
        return tlog.getGtrid().toString();
    }

}
