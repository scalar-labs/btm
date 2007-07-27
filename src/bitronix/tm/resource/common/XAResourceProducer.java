package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;

import javax.naming.Referenceable;
import javax.transaction.xa.XAResource;
import java.io.Serializable;

/**
 * All XA-capable wrappers must implement this interface.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public interface XAResourceProducer extends Referenceable, Serializable {

    /**
     * Get the resource name as registered in the transactions journal.
     * @return the unique name of the resource.
     */
    public String getUniqueName();

    /**
     * Prepare the recoverable {@link XAResource} producer for recovery.
     * @return a {@link XAResourceHolderState} object that can be used to call <code>recover()</code>.
     */
    public XAResourceHolderState startRecovery();

    /**
     * Release internal resources held after call to <code>startRecovery()</code>.
     */
    public void endRecovery();

    /**
     * Find in the {@link XAResourceHolder}s created by this {@link XAResourceProducer} the one which this
     * {@link XAResource} belongs to.
     * @param xaResource the {@link XAResource} to look for.
     * @return the associated {@link XAResourceHolder} or null if the {@link XAResource} does not belong to this
     *         {@link XAResourceProducer}.
     */
    public XAResourceHolder findXAResourceHolder(XAResource xaResource);

    /**
     * Initialize this {@link XAResourceProducer}'s internal resources.
     */
    public void init();

    /**
     * Release this {@link XAResourceProducer}'s internal resources.
     */
    public void close();

    /**
     * Create a {@link XAStatefulHolder} that will be placed in an {@link XAPool}.
     * @param xaFactory the vendor's resource-specific XA factory.
     * @param bean the resource-specific bean describing the resource parameters.
     * @return a {@link XAStatefulHolder} that will be placed in an {@link XAPool}.
     * @throws Exception thrown when the {@link XAStatefulHolder} cannot be created.
     */
    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception;

}
