package bitronix.tm.mock.events;

import javax.transaction.xa.Xid;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public abstract class XAEvent extends Event {

    private Xid xid;

    protected XAEvent(Object source, Xid xid) {
        super(source, null);
        this.xid = xid;
    }

    protected XAEvent(Object source, Exception ex, Xid xid) {
        super(source, ex);
        this.xid = xid;
    }

    public Xid getXid() {
        return xid;
    }

}
