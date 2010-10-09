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

import bitronix.tm.journal.TransactionLogCursor;
import bitronix.tm.journal.TransactionLogRecord;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.swing.table.TableModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p></p>
 *
 * @author lorban
 */
public abstract class TransactionTableModel implements TableModel {

    private final static Logger log = LoggerFactory.getLogger(TransactionTableModel.class);

    protected List tLogs = new ArrayList();

    protected void readFullTransactionLog(File filename) throws IOException {
        TransactionLogCursor tlis = new TransactionLogCursor(filename);

        int count=0;
        try {
            while (true) {
                TransactionLogRecord tlog = tlis.readLog(true);
                if (tlog == null)
                    break;
                if (!acceptLog(tlog))
                    continue;
                tLogs.add(tlog);
                count++;
            }
        }
        finally {
            tlis.close();
            if (log.isDebugEnabled()) log.debug("read " + count + " transaction logs");
        }
    }

    public abstract boolean acceptLog(TransactionLogRecord tlog);

    public abstract TransactionLogRecord getRow(int row);

}
