package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Uid;

import javax.transaction.xa.XAResource;

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
     * @param gtrid the GTRID of the transaction state to add.
     * @return the {@link XAResourceHolderState}.
     */
    public XAResourceHolderState getXAResourceHolderState(Uid gtrid);

    /**
     * Set the {@link XAResourceHolderState} of this wrapped resource.
     * @param gtrid the GTRID of the transaction state to add.
     * @param xaResourceHolderState the {@link XAResourceHolderState} to set.
     */
    public void putXAResourceHolderState(Uid gtrid, XAResourceHolderState xaResourceHolderState);


    /**
     * Remove the specified state from this wrapped resource.
     * @param gtrid the GTRID of the transaction state to remove.
     * @return true if the state actually existed before removal, false otherwise.
     */
    public void removeXAResourceHolderState(Uid gtrid);

    /**
     * Check if this {@link XAResourceHolder} contains a state for a specific {@link XAResourceHolder}.
     * In other words: has the {@link XAResourceHolder}'s {@link XAResource} been enlisted in some transaction ?
     * @param xaResourceHolder the {@link XAResourceHolder} to look for.
     * @return true if the {@link XAResourceHolder} is enlisted in some transaction, false otherwise.
     */
    public boolean hasStateForXAResource(XAResourceHolder xaResourceHolder);

    public ResourceBean getResourceBean();

}
