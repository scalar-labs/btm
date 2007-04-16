package bitronix.tm.recovery;

/**
 * Thrown when an error occurs during recovery.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class RecoveryException extends RuntimeException {
    public RecoveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecoveryException(String message) {
        super(message);
    }
}
