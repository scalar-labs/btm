package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;

/**
 * Implementation of all services required by a {@link XAResourceHolder}.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
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
    
}
