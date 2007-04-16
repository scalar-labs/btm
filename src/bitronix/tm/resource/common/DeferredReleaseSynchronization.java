package bitronix.tm.resource.common;

import javax.transaction.Synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Synchronization} used to release a {@link XAStatefulHolder} object after 2PC has executed.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class DeferredReleaseSynchronization implements Synchronization {

    private final static Logger log = LoggerFactory.getLogger(DeferredReleaseSynchronization.class);

    private XAStatefulHolder xaStatefulHolder;

    public DeferredReleaseSynchronization(XAStatefulHolder xaStatefulHolder) {
        this.xaStatefulHolder = xaStatefulHolder;
    }

    public XAStatefulHolder getXAStatefulHolder() {
        return xaStatefulHolder;
    }

    public void afterCompletion(int status) {
        if (log.isDebugEnabled()) log.debug("DeferredReleaseSynchronization requeuing " + xaStatefulHolder);
        xaStatefulHolder.setState(XAResourceHolder.STATE_IN_POOL);
        if (log.isDebugEnabled()) log.debug("DeferredReleaseSynchronization requeued " + xaStatefulHolder);
    }

    public void beforeCompletion() {
    }

    public String toString() {
        return "a DeferredReleaseSynchronization of " + xaStatefulHolder;
    }
}