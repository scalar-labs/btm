package bitronix.tm.mock.events;

import bitronix.tm.internal.Decoder;

import javax.transaction.xa.Xid;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class XAResourcePrepareEvent extends XAEvent {

    private int returnCode;

    public XAResourcePrepareEvent(Object source, Xid xid, int returnCode) {
        super(source, xid);
        this.returnCode = returnCode;
    }

    public XAResourcePrepareEvent(Object source, Exception ex, Xid xid, int returnCode) {
        super(source, ex, xid);
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public String toString() {
        return "XAResourcePrepareEvent at " + getTimestamp() + " with vote=" + Decoder.decodePrepareVote(returnCode) + " on " + getXid();
    }
}
