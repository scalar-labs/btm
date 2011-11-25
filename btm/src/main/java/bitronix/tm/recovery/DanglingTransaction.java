/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.recovery;

import javax.transaction.xa.Xid;

/**
 * Simple bean containing a unique resource name paired with a XID corresponding to a branch on that resource.
 *
 * @author lorban
 */
public final class DanglingTransaction {

    private final String uniqueName;
    private final Xid xid;

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
