package bitronix.tm.mock.events;

import javax.transaction.xa.Xid;

import bitronix.tm.internal.Decoder;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class XAResourceStartEvent extends XAEvent {

    private int flag;

    public XAResourceStartEvent(Object source, Xid xid, int flag) {
        super(source, xid);
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }

    public String toString() {
        return "XAResourceStartEvent at " + getTimestamp() + " with flag=" + Decoder.decodeXAResourceFlag(flag) + " on " + getXid();
    }
}
