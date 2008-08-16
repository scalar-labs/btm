package bitronix.tm.resource;

/**
 * Thrown when a resource cannot be created due to a configuration error.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class ResourceConfigurationException extends RuntimeException {
    public ResourceConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceConfigurationException(String s) {
        super(s);
    }
}
