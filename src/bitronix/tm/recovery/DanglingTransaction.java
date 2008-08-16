package bitronix.tm.recovery;

import javax.transaction.xa.Xid;

/**
 * Simple bean containing a unique resource name paired with a XID corresponding to a branch on that resource.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class DanglingTransaction {

    private String uniqueName;
    private Xid xid;

    public DanglingTransaction(String uniqueName, Xid xid) {
        if (uniqueName == null)
            throw new NullPointerException("uniqueName cannot be null");
        if (xid == null)
            throw new NullPointerException("xid cannot be null");
        this.uniqueName = uniqueName;
        this.xid = xid;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public Xid getXid() {
        return xid;
    }

    public int hashCode() {
        return uniqueName.hashCode() + xid.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof DanglingTransaction) {
            DanglingTransaction otherDanglingTransaction = (DanglingTransaction) obj;

            return uniqueName.equals(otherDanglingTransaction.uniqueName) &&
                    xid.equals(otherDanglingTransaction.xid);
        }
        return false;
    }

}
