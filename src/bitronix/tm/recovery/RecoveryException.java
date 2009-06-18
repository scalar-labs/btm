package bitronix.tm.recovery;

/**
 * Thrown when an error occurs during recovery.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class RecoveryException extends Exception {
    public RecoveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecoveryException(String message) {
        super(message);
    }
}
