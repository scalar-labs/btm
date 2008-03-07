package bitronix.tm.mock.events;

import bitronix.tm.utils.Decoder;

import javax.transaction.xa.Xid;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class XAResourceEndEvent extends XAEvent {

    private int flag;

    public XAResourceEndEvent(Object source, Xid xid, int flag) {
        super(source, xid);
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }

    public String toString() {
        return "XAResourceEndEvent at " + getTimestamp() + " with flag=" + Decoder.decodeXAResourceFlag(flag) + " on " + getXid();
    }

}
