package bitronix.tm.resource.common;

import java.util.List;

/**
 * Wrappers of a poolable XA object must implement this interface. It defines all the services that must be
 * implemented by the wrapper as well as the pooling lifecycle states.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public interface XAStatefulHolder {

    /**
     * The state in which the resource is when it is closed and unusable.
     */
    public final static int STATE_CLOSED = 0;

    /**
     * The state in which the resource is when it is available in the pool.
     */
    public final static int STATE_IN_POOL = 1;

    /**
     * The state in which the resource is when it out of the pool but accessible by the application.
     */
    public final static int STATE_ACCESSIBLE = 2;

    /**
     * The state in which the resource is when it out of the pool but not accessible by the application.
     */
    public final static int STATE_NOT_ACCESSIBLE = 3;


    /**
     * Get the current resource state.
     * <p>This method is thread-safe.</p>
     * @return the current resource state.
     */
    public int getState();

    /**
     * Set the current resource state.
     * <p>This method is thread-safe.</p>
     * @param state the current resource state.
     */
    public void setState(int state);

    /**
     * Register an implementation of {@link StateChangeListener}.
     * @param listener the {@link StateChangeListener} implementation to register.
     */
    public void addStateChangeEventListener(StateChangeListener listener);

    /**
     * Unregister an implementation of {@link StateChangeListener}.
     * @param listener the {@link StateChangeListener} implementation to unregister.
     */
    public void removeStateChangeEventListener(StateChangeListener listener);

    /**
     * Get the list of {@link bitronix.tm.resource.common.XAResourceHolder}s created by this
     * {@link bitronix.tm.resource.common.XAStatefulHolder} that are still open.
     * <p>This method is thread-safe.</p>
     * @return the list of {@link XAResourceHolder}s created by this
     *         {@link bitronix.tm.resource.common.XAStatefulHolder} that are still open.
     */
    public List getXAResourceHolders();

    /**
     * Create a disposable handler used to drive a pooled instance of
     * {@link bitronix.tm.resource.common.XAStatefulHolder}.
     * <p>This method is thread-safe.</p>
     * @return a resource-specific disaposable connection object.
     * @throws Exception a resource-specific exception thrown when the disaposable connection cannot be created.
     */
    public Object getConnectionHandle() throws Exception;

    /**
     * Close the physical connection implementing {@link bitronix.tm.resource.common.XAStatefulHolder}.
     * @throws Exception a resource-specific exception thrown when there is an error closing the physical connection.
     */
    public void close() throws Exception;

}
