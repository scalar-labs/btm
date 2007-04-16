package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;

import javax.transaction.xa.XAResource;

/**
 * {@link XAResource} wrappers must implement this interface. It defines all the services that must be implemented by
 * the wrapper.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
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
     * Should the TM use the Last Resource Commit optimization with that resource ?
     * @return true if Last Resource Commit should be used with that resource, false otherwise.
     */
    public boolean isEmulatingXA();

}
