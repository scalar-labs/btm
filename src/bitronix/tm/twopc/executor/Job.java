package bitronix.tm.twopc.executor;

import bitronix.tm.internal.XAResourceHolderState;

import javax.transaction.xa.XAException;

/**
 * Abstract job definition executable by the 2PC thread pools.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public abstract class Job implements Runnable {
    private Object future;
    private XAResourceHolderState resourceHolder;

    protected XAException xaException;
    protected RuntimeException runtimeException;

    public Job(XAResourceHolderState resourceHolder) {
        this.resourceHolder = resourceHolder;
    }

    public XAResourceHolderState getResource() {
        return resourceHolder;
    }

    public XAException getXAException() {
        return xaException;
    }

    public RuntimeException getRuntimeException() {
        return runtimeException;
    }

    public void setFuture(Object future) {
        this.future = future;
    }

    public Object getFuture() {
        return future;
    }
}
