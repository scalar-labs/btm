package bitronix.tm.internal;

/**
 * Thrown by {@link PropertyUtils} when some reflection error occurs.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
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
