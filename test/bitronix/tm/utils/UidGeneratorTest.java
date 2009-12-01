package bitronix.tm.utils;

import junit.framework.TestCase;

import java.util.HashSet;

import bitronix.tm.utils.Encoder;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;

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

}
