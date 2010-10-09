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
package bitronix.tm.mock.events;

import javax.transaction.xa.Xid;

/**
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
