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
package bitronix.tm.utils;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper that offers UID generation (GTRID, XID, sequences) needed by the transaction manager.
 * <p>Generated UIDs are at most 64 bytes long and are made of 3 subparts: the current time in milliseconds since
 * Epoch, a JVM transient atomic sequence number and the configured <code>bitronix.tm.serverId</code>.</p>
 * <p>The reliance on the system clock is critical to the uniqueness of the UID in the network so you have to make sure
 * all servers of the network running this transaction manager have their clock reasonably in sync. An order of 1
 * second synchronicity is generally fine.</p>
 *
 * @author lorban
 */
public class UidGenerator {

    private final static AtomicInteger sequenceGenerator = new AtomicInteger();

    /**
     * Generate a UID, globally unique. This method relies on the configured serverId for network uniqueness.
     * @return the generated UID.
     */
    public static Uid generateUid() {
        byte[] timestamp = Encoder.longToBytes(MonotonicClock.currentTimeMillis());
        byte[] sequence = Encoder.intToBytes(sequenceGenerator.incrementAndGet());
        byte[] serverId = TransactionManagerServices.getConfiguration().buildServerIdArray();

        int uidLength = serverId.length + timestamp.length + sequence.length;
        byte[] uidArray = new byte[uidLength];

        System.arraycopy(serverId, 0, uidArray, 0, serverId.length);
        System.arraycopy(timestamp, 0, uidArray, serverId.length, timestamp.length);
        System.arraycopy(sequence, 0, uidArray, serverId.length + timestamp.length, sequence.length);

        return new Uid(uidArray);
    }

    /**
     * Generate a XID with the specified globalTransactionId.
     * @param gtrid the GTRID to use to generate the Xid.
     * @return the generated Xid.
     */
    public static BitronixXid generateXid(Uid gtrid) {
        return new BitronixXid(gtrid, generateUid());
    }

}
