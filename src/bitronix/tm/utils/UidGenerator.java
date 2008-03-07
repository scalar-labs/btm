package bitronix.tm.utils;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;

/**
 * Helper that offers UID generation (GTRID, XID, sequences) needed by the transaction manager.
 * <p>Generated UIDs are at most 64 bytes long and are made of 3 subparts: the current time in milliseconds since
 * Epoch, a JVM transient atomic sequence number and the configured <code>bitronix.tm.serverId</code>.</p>
 * <p>The reliance on the system clock is critical to the uniqueness of the UID in the network so you have to make sure
 * all servers of the network running this transaction manager have their clock reasonably in sync. An order of 1
 * second synchronicity is generally fine.</p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class UidGenerator {

    private static int sequenceNumber = 0;

    /**
     * Generate a UID, globally unique. This method relies on the configured serverId for network uniqueness.
     * @return the generated UID.
     */
    public static Uid generateUid() {
        byte[] timestamp = Encoder.longToBytes(System.currentTimeMillis());
        byte[] sequence = Encoder.intToBytes(getNextSequenceNumber());

        int uidLength = getServerId().length + timestamp.length + sequence.length;
        byte[] uidArray = new byte[uidLength];

        System.arraycopy(getServerId(), 0, uidArray, 0, getServerId().length);
        System.arraycopy(timestamp, 0, uidArray, getServerId().length, timestamp.length);
        System.arraycopy(sequence, 0, uidArray, getServerId().length + timestamp.length, sequence.length);

        return new Uid(uidArray);
    }

    private static byte[] getServerId() {
        return TransactionManagerServices.getConfiguration().buildServerIdArray();
    }

    /**
     * Atomically generate general-purpose sequence numbers starting at 0. The counter is reset at every
     * JVM startup.
     * @return a sequence number unique for the lifespan of this JVM.
     */
    public static synchronized int getNextSequenceNumber() {
        return sequenceNumber++;
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
