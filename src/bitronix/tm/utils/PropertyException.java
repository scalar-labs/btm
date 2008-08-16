package bitronix.tm.utils;

/**
 * Thrown by {@link PropertyUtils} when some reflection error occurs.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class PropertyException extends RuntimeException {

    public PropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyException(String message) {
        super(message);
    }

}
