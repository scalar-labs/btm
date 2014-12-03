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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.TableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.journal.TransactionLogCursor;
import bitronix.tm.journal.TransactionLogRecord;

/**
 * @author Ludovic Orban
 */
public abstract class TransactionTableModel implements TableModel {

    private final static Logger log = LoggerFactory.getLogger(TransactionTableModel.class);

    protected List<TransactionLogRecord> tLogs = new ArrayList<TransactionLogRecord>();

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
            if (log.isDebugEnabled()) { log.debug("read " + count + " transaction logs"); }
        }
    }

    public abstract boolean acceptLog(TransactionLogRecord tlog);

    public abstract TransactionLogRecord getRow(int row);

}
