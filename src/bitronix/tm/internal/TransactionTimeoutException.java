package bitronix.tm.internal;

/**
 * Thrown when a transaction times out.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
