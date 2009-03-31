package bitronix.tm.utils;

/**
 * Number to byte array encoder.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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

    public static long bytesToLong(byte[] bytes) {
        if (bytes.length != 8)
            throw new IllegalArgumentException("a long can only be decoded from a 8 bytes array (got a " + bytes.length + " byte(s) array)");

        long result = 0;

        for(int i=0; i < 8 ;i++) {
           result <<= 8;
           result ^= (long) bytes[i] & 0xFF;
        }

        return result;
    }
}
