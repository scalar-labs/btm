package bitronix.tm.resource.common;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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

    private final static Logger log = LoggerFactory.getLogger(AbstractXAResourceHolder.class);

    private final Map xaResourceHolderStates = Collections.synchronizedMap(new HashMap());

    public XAResourceHolderState getXAResourceHolderState(Uid gtrid) {
        synchronized (xaResourceHolderStates) {
            return (XAResourceHolderState) xaResourceHolderStates.get(gtrid);
        }
    }

    public void putXAResourceHolderState(Uid gtrid, XAResourceHolderState xaResourceHolderState) {
        synchronized (xaResourceHolderStates) {
            if (log.isDebugEnabled()) log.debug("putting XAResourceHolderState [" + xaResourceHolderState + "] of GTRID [" + gtrid + "] on " + this);
            if (!xaResourceHolderStates.containsKey(gtrid)) {
                if (log.isDebugEnabled()) log.debug("GTRID [" + gtrid + "] previously unknown to " + this + ", adding it to the resource's transactions list");
                xaResourceHolderStates.put(gtrid, xaResourceHolderState);
            }
            else log.warn("tried to put again known GTRID [" + gtrid + "] on " + this + " - Bug?");
        }
    }

    public void removeXAResourceHolderState(Uid gtrid) {
        synchronized (xaResourceHolderStates) {
            if (log.isDebugEnabled()) log.debug("removing XAResourceHolderState of GTRID [" + gtrid + "] from " + this);
            Object removed = xaResourceHolderStates.remove(gtrid);
            if (removed == null) log.warn("tried to remove unknown GTRID [" + gtrid + "] from " + this + " - Bug?");
        }
    }

    public boolean hasStateForXAResource(XAResourceHolder xaResourceHolder) {
        synchronized (xaResourceHolderStates) {
            Iterator it = xaResourceHolderStates.values().iterator();
            while (it.hasNext()) {
                XAResourceHolderState otherXaResourceHolderState = (XAResourceHolderState) it.next();

                if (otherXaResourceHolderState.getXAResource() == xaResourceHolder.getXAResource()) {
                    if (log.isDebugEnabled()) log.debug("resource " + xaResourceHolder + " is enlisted in another transaction with " + otherXaResourceHolderState.getXid().toString());
                    return true;
                }
            }

            if (log.isDebugEnabled()) log.debug("resource not enlisted in any transaction: " + xaResourceHolder);
            return false;
        }
    }

    /**
     * If this method returns false, then local transaction calls like Connection.commit() can be made.
     * @return true if start() has been successfully called but not end() yet <i>and</i> the transaction is not suspended.
     */
    public boolean isParticipatingInActiveGlobalTransaction() {
        synchronized (xaResourceHolderStates) {
            BitronixTransaction currentTransaction = TransactionContextHelper.currentTransaction();
            Uid gtrid = currentTransaction == null ? null : currentTransaction.getResourceManager().getGtrid();
            if (gtrid == null)
                return false;

            XAResourceHolderState xaResourceHolderState = getXAResourceHolderState(gtrid);
            return xaResourceHolderState != null &&
                    xaResourceHolderState.isStarted() &&
                    !xaResourceHolderState.isSuspended() &&
                    !xaResourceHolderState.isEnded();
        }
    }

    /**
     * Simple helper method which returns a set of GTRIDs of transactions in which
     * this resource is enlisted. Useful for monitoring.
     * @return a set of GTRIDs of transactions in which this resource is enlisted.
     */
    public Set getXAResourceHolderStateGtrids() {
        synchronized (xaResourceHolderStates) {
            return new HashSet(xaResourceHolderStates.keySet());
        }
    }
}
