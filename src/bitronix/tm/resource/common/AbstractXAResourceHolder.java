package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;

import java.util.List;
import java.util.ArrayList;

/**
 * Implementation of all services required by a {@link XAResourceHolder}. This class keeps a list of all
 * {@link XAResourceHolderState}s of the {@link XAResourceHolder} plus the currently active one. There is
 * one per transaction in which this {@link XAResourceHolder} is enlisted plus all the suspended transactions in which
 * it is enlisted as well.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public abstract class AbstractXAResourceHolder extends AbstractXAStatefulHolder implements XAResourceHolder {

    private XAResourceHolderState currentXaResourceHolderState;
    private List xaResourceHolderStates = new ArrayList();

    public XAResourceHolderState getXAResourceHolderState() {
        return currentXaResourceHolderState;
    }

    public void setXAResourceHolderState(XAResourceHolderState xaResourceHolderState) {
        if (xaResourceHolderState != null) {
            this.currentXaResourceHolderState = xaResourceHolderState;
            if (!xaResourceHolderStates.contains(xaResourceHolderState))
                this.xaResourceHolderStates.add(xaResourceHolderState);
        }
        else {
            xaResourceHolderStates.remove(currentXaResourceHolderState);
            this.currentXaResourceHolderState = null;
        }
    }

    public List getAllXAResourceHolderStates() {
        return xaResourceHolderStates;
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
