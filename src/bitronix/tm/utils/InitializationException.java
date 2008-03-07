package bitronix.tm.utils;

/**
 * Thrown at transaction manager startup when an error occurs.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
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
