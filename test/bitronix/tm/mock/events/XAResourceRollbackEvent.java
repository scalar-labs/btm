package bitronix.tm.mock.events;

import javax.transaction.xa.Xid;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class XAResourceRollbackEvent extends XAEvent {

    public XAResourceRollbackEvent(Object source, Xid xid) {
        super(source, xid);
    }

    public XAResourceRollbackEvent(Object source, Exception ex, Xid xid) {
        super(source, ex, xid);
    }

    public String toString() {
        return "XAResourceRollbackEvent at " + getTimestamp() + " on " + getXid();
    }
}
