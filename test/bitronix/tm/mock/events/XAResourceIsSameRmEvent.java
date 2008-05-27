package bitronix.tm.mock.events;

import javax.transaction.xa.XAResource;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class XAResourceIsSameRmEvent extends XAEvent {

    private XAResource xaResource;

    public XAResourceIsSameRmEvent(Object source, XAResource xaResource) {
        super(source, null);
        this.xaResource = xaResource;
    }

    public XAResource getXAResource() {
        return xaResource;
    }

    public String toString() {
        return "XAResourceIsSameRmEvent at " + getTimestamp() + " with XAResource=" + xaResource;
    }

}