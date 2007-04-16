package bitronix.tm.mock.events;

import javax.transaction.xa.Xid;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class XAResourceForgetEvent extends XAEvent {

    public XAResourceForgetEvent(Object source, Xid xid) {
        super(source, xid);
    }

    public String toString() {
        return "XAResourceForgetEvent at " + getTimestamp() + " on " + getXid();
    }
}
