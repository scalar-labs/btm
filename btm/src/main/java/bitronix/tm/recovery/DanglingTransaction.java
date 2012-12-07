/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
