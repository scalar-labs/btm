package bitronix.tm.journal;

import java.io.IOException;

/**
 * Thrown by {@link TransactionLogCursor} when an integrity check fails upon reading a record.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class CorruptedTransactionLogException extends IOException {
    public CorruptedTransactionLogException(String s) {
        super(s);
    }
}
