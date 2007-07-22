package bitronix.tm.internal;

/**
 * Thrown when a runtime exception happens.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixRuntimeException extends RuntimeException {
    public BitronixRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
    public BitronixRuntimeException(String message) {
        super(message);
    }
}
