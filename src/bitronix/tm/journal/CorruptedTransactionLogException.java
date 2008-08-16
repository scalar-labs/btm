package bitronix.tm.journal;

import java.io.IOException;

/**
 * Thrown by {@link TransactionLogCursor} when an integrity check fails upon reading a record.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class CorruptedTransactionLogException extends IOException {
    public CorruptedTransactionLogException(String s) {
        super(s);
    }
}
