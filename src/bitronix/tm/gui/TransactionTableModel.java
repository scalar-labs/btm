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
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
