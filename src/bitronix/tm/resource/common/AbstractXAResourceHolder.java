package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;

/**
 * Implementation of all services required by a {@link XAResourceHolder}.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public abstract class AbstractXAResourceHolder extends AbstractXAStatefulHolder implements XAResourceHolder {

    protected XAResourceHolderState xaResourceHolderState;

    public XAResourceHolderState getXAResourceHolderState() {
        return xaResourceHolderState;
    }

    public void setXAResourceHolderState(XAResourceHolderState xaResourceHolderState) {
        this.xaResourceHolderState = xaResourceHolderState;
    }

    /**
     * If this method returns false, then local transaction calls like Connection.commit() can be made.
     * @return true if start() has been successfully called but not end() yet <i>and</i> the transaction is not suspended.
     */
    public boolean isParticipatingInActiveGlobalTransaction() {
        XAResourceHolderState xaResourceHolderState = getXAResourceHolderState();
        return xaResourceHolderState != null &&
                xaResourceHolderState.isStarted() &&
                !xaResourceHolderState.isSuspended() &&
                !xaResourceHolderState.isEnded();
    }
    
}
