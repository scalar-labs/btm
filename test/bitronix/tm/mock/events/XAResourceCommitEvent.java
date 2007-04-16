package bitronix.tm.mock.events;

import javax.transaction.xa.Xid;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class XAResourceCommitEvent extends XAEvent {

    private boolean onePhase;

    public XAResourceCommitEvent(Object source, Xid xid, boolean onePhase) {
        super(source, xid);
        this.onePhase = onePhase;
    }

    public XAResourceCommitEvent(Object source, Exception ex, Xid xid, boolean onePhase) {
        super(source, ex, xid);
        this.onePhase = onePhase;
    }

    public boolean isOnePhase() {
        return onePhase;
    }

    public String toString() {
        return "XAResourceCommitEvent at " + getTimestamp() + " with onePhase=" + onePhase + (getException()!=null ? " and " + getException().toString() : "" + " on " + getXid());
    }
}
