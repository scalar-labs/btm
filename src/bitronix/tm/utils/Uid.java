package bitronix.tm.utils;

import bitronix.tm.TransactionManagerServices;

import java.util.Arrays;

/**
 * <p>A constant UID byte array container optimized for use with hashed collections.</p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public final class Uid {

    private byte[] array;
    private int hashCodeValue;
    private String toStringValue;

    public Uid(byte[] array) {
        this.array = array;
        this.hashCodeValue = arrayHashCode(array);
        this.toStringValue = arrayToString(array);
    }

    public byte[] getArray() {
        return array;
    }

    public byte[] extractServerId() {
        int serverIdLength = array.length - 8 - 4;
        if (serverIdLength < 1)
            return null;

        byte[] result = new byte[serverIdLength];
        System.arraycopy(array, 0, result, 0, serverIdLength);
        return result;
    }

    public long extractTimestamp() {
        return Encoder.bytesToLong(array,  TransactionManagerServices.getConfiguration().buildServerIdArray().length);
    }

    public boolean equals(Object obj) {
        if (obj instanceof Uid) {
            Uid otherUid = (Uid) obj;

            // optimizes performance a bit
            if (hashCodeValue != otherUid.hashCodeValue)
                return false;

            return Arrays.equals(array, otherUid.array);
        }
        return false;
    }

    public int hashCode() {
        return hashCodeValue;
    }

    public String toString() {
        return toStringValue;
    }

    /**
     * Compute a UID byte array hashcode value.
     * @param uid the byte array used for hashcode computation.
     * @return a constant hash value for the specified uid.
     */
    private static int arrayHashCode(byte[] uid) {
        int total = 0;
        for (int i = 0; i < uid.length; i++) {
            total += uid[i];
        }
        return total;
    }

    /**
     * Decode a UID byte array into a (somewhat) human-readable hex string.
     * @param uid the uid to decode.
     * @return the resulting printable string.
     */
    private static String arrayToString(byte[] uid) {
        StringBuffer sb = new StringBuffer(uid.length * 2);
        for (int i = 0; i < uid.length ; i++) {
            byte b = uid[i];
            sb.append(toHexString(b));
        }
        return sb.toString();
    }

    private static String toHexString(int b) {
        int val = b;
        if (val < 0)
            val = val + 256;
        return BYTES_TABLE[val];
    }

    private final static String[] BYTES_TABLE = new String[] {
        "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
        "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
        "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
        "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
        "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "4A", "4B", "4C", "4D", "4E", "4F",
        "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "5A", "5B", "5C", "5D", "5E", "5F",
        "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "6A", "6B", "6C", "6D", "6E", "6F",
        "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "7A", "7B", "7C", "7D", "7E", "7F",
        "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "8A", "8B", "8C", "8D", "8E", "8F",
        "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "9A", "9B", "9C", "9D", "9E", "9F",
        "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF",
        "B0", "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9", "BA", "BB", "BC", "BD", "BE", "BF",
        "C0", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "CA", "CB", "CC", "CD", "CE", "CF",
        "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "DA", "DB", "DC", "DD", "DE", "DF",
        "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF",
        "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "FA", "FB", "FC", "FD", "FE", "FF"
    };

}
