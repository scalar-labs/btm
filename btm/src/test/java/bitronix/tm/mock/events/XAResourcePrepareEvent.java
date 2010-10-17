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

import bitronix.tm.utils.Decoder;

import javax.transaction.xa.Xid;

/**
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
