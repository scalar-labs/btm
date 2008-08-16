package bitronix.tm.utils;

/**
 * All internal services implement this interface.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @see bitronix.tm.TransactionManagerServices
 * @author lorban
 */
public interface Service {

    /**
     * Shutdown the service and free all held resources.
     */
    public void shutdown();

}
