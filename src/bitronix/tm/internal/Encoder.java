package bitronix.tm.internal;

/**
 * Numbers to byte arrays encoder.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class Encoder {

    public static byte[] longToBytes(long aLong) {
        byte[] array = new byte[8];

        for (int i=0; i<array.length ;i++) {
            array[array.length - (i+1)] = (byte) ((aLong >> (8 * i)) & 0xff);
        }

        return array;
    }

    public static byte[] intToBytes(long anInt) {
        byte[] array = new byte[4];

        for (int i=0; i<array.length ;i++) {
            array[array.length - (i+1)] = (byte) ((anInt >> (8 * i)) & 0xff);
        }

        return array;
    }

    public static byte[] shortToBytes(short aShort) {
        byte[] array = new byte[2];

        for (int i=0; i<array.length ;i++) {
            array[array.length - (i+1)] = (byte) ((aShort >> (8 * i)) & 0xff);
        }

        return array;
    }
}
