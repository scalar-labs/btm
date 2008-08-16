package bitronix.tm.utils;

/**
 * Thrown at transaction manager startup when an error occurs.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class InitializationException extends RuntimeException {
    public InitializationException(String message) {
        super(message);
    }

    public InitializationException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
