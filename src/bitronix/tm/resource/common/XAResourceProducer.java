package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;

import javax.naming.Referenceable;
import javax.transaction.xa.XAResource;
import java.io.Serializable;

/**
 * A {@link XAResourceProducer} is a {@link XAStatefulHolder} factory. It must be implemented by any class that is
 * able to produce pooled XA connections.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
     * @throws bitronix.tm.recovery.RecoveryException thrown when a {@link XAResourceHolderState} cannot be acquired.
     */
    public XAResourceHolderState startRecovery() throws RecoveryException;

    /**
     * Release internal resources held after call to <code>startRecovery()</code>.
     * @throws bitronix.tm.recovery.RecoveryException thrown when an error occured while releasing reserved resources.
     */
    public void endRecovery() throws RecoveryException;

    /**
     * Mark this resource producer as failed or not. A resource is considered failed if recovery fails to run on it.
     * @param failed true is the resource must be considered failed, false it it must be considered sane.
     */
    public void setFailed(boolean failed);

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
