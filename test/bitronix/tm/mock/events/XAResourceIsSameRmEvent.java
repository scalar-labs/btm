package bitronix.tm.mock.events;

import javax.transaction.xa.XAResource;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class XAResourceIsSameRmEvent extends XAEvent {

    private XAResource xaResource;
    private boolean sameRm;

    public XAResourceIsSameRmEvent(Object source, XAResource xaResource, boolean sameRm) {
        super(source, null);
        this.xaResource = xaResource;
        this.sameRm = sameRm;
    }

    public XAResource getXAResource() {
        return xaResource;
    }

    public boolean isSameRm() {
        return sameRm;
    }

    public String toString() {
        return "XAResourceIsSameRmEvent at " + getTimestamp() + " with XAResource=" + xaResource;
    }

}