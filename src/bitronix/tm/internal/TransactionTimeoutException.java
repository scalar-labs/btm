package bitronix.tm.internal;

/**
 * Thrown when a transaction times out.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class TransactionTimeoutException extends Exception {
    public TransactionTimeoutException(String message) {
        super(message);
    }

    public TransactionTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
