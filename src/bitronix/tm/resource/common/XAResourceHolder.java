package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;

import javax.transaction.xa.XAResource;
import java.util.List;

/**
 * {@link XAResource} wrappers must implement this interface. It defines a way to get access to the transactional
 * state of this {@link XAResourceHolder}.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @see XAResourceHolderState
 * @author lorban
 */
public interface XAResourceHolder extends XAStatefulHolder {

    /**
     * Get the vendor's {@link XAResource} implementation of the wrapped resource.
     * @return the vendor's XAResource implementation.
     */
    public XAResource getXAResource();

    /**
     * Get the {@link XAResourceHolderState} of this wrapped resource.
     * <p>Since a {@link XAResourceHolder} can participate in more than one transaction at a time (when suspending a
     * context for instance) the transaction manager guarantees that the {@link XAResourceHolderState} related to the
     * current transaction context will be returned.</p>
     * @return the {@link XAResourceHolderState}.
     */
    public XAResourceHolderState getXAResourceHolderState();

    /**
     * Set the {@link XAResourceHolderState} of this wrapped resource.
     * @param xaResourceHolderState the {@link XAResourceHolderState}.
     */
    public void setXAResourceHolderState(XAResourceHolderState xaResourceHolderState);

    /**
     * Get a {@link List} of all existing {@link XAResourceHolderState}s for this {@link XAResourceHolder}. Basically
     * there is one entry in the list per in-flight transaction in which this {@link XAResource} is enlisted.
     * @return a {@link List} of all existing {@link XAResourceHolderState}s for this {@link XAResourceHolder}.
     */
    public List getAllXAResourceHolderStates();

}
