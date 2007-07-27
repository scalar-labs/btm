package bitronix.tm.internal;

/**
 * All internal services implement this interface.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public interface Service {

    /**
     * Shutdown the service and free all held resources.
     */
    public void shutdown();

}
