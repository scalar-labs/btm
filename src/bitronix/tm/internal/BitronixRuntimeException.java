package bitronix.tm.internal;

/**
 * Thrown when a runtime exception happens.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
