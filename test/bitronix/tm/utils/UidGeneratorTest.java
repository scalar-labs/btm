package bitronix.tm.utils;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashSet;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class UidGeneratorTest extends TestCase {


    public void testHexaStringEncoder() throws Exception {
        byte[] result = Encoder.intToBytes(0x80);
        String hexString = new Uid(result).toString();
        assertEquals("00000080", hexString);

        result = Encoder.longToBytes(0x81);
        hexString = new Uid(result).toString();
        assertEquals("0000000000000081", hexString);

        result = Encoder.shortToBytes((short)0xff);
        hexString = new Uid(result).toString();
        assertEquals("00FF", hexString);
    }


    public void testUniqueness() throws Exception {
        final int count = 10000;
        HashSet uids = new HashSet(2048);

        for (int i=0; i<count ;i++) {
            Uid uid = UidGenerator.generateUid();
            assertTrue("UidGenerator generated duplicate UID at #" + i, uids.add(uid.toString()));
        }
    }

    public void testEquals() throws Exception {
        Uid uid1 = UidGenerator.generateUid();
        Uid uid2 = UidGenerator.generateUid();
        Uid uid3 = null;

        assertFalse(uid1.equals(uid2));
        assertFalse(uid2.equals(uid3));
        assertTrue(uid2.equals(uid2));
    }
    
    public void testExtracts() throws Exception {
        byte[] timestamp = Encoder.longToBytes(System.currentTimeMillis());
        byte[] sequence = Encoder.intToBytes(1);
        byte[] serverId = "my-server-id".getBytes();

        int uidLength = serverId.length + timestamp.length + sequence.length;
        byte[] uidArray = new byte[uidLength];

        System.arraycopy(serverId, 0, uidArray, 0, serverId.length);
        System.arraycopy(timestamp, 0, uidArray, serverId.length, timestamp.length);
        System.arraycopy(sequence, 0, uidArray, serverId.length + timestamp.length, sequence.length);

        Uid uid = new Uid(uidArray);

        assertTrue(Arrays.equals(serverId, uid.extractServerId()));
        assertEquals(Encoder.bytesToLong(timestamp, 0), uid.extractTimestamp());
        assertEquals(Encoder.bytesToInt(sequence, 0), uid.extractSequence());
    }

}
