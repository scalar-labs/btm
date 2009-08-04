package bitronix.tm.utils;

import bitronix.tm.internal.BitronixRuntimeException;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * <p>Simple crypto helper that uses symetric keys to crypt and decrypt resources passwords.</p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class CryptoEngine {

    private static final int LONG_SIZE_IN_BYTES = 8;
    private static final String CRYPTO_PASSWORD = "B1tr0n!+";

    /**
     * Crypt the given data using the given cipher.
     * The crypted result is base64-encoded before it is returned.
     * @param cipher the cypther to use.
     * @param data the data to crypt.
     * @return crypted, base64-encoded data.
     * @throws InvalidKeyException if the given key material is shorter than 8 bytes.
     * @throws NoSuchAlgorithmException if a secret-key factory for the specified algorithm is not available in the
     *         default provider package or any of the other provider packages that were searched.
     * @throws NoSuchPaddingException if transformation contains a padding scheme that is not available.
     * @throws InvalidKeySpecException if the given key specification is inappropriate for this secret-key factory to
     *         produce a secret key.
     * @throws IOException if an I/O error occurs.
     */
    public static String crypt(String cipher, String data) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, IOException {
        byte[] prependedBytes = Encoder.longToBytes(System.currentTimeMillis());

        byte[] dataBytes = data.getBytes("US-ASCII");
        byte[] toCrypt = new byte[LONG_SIZE_IN_BYTES + dataBytes.length];
        System.arraycopy(prependedBytes, 0, toCrypt, 0, LONG_SIZE_IN_BYTES);
        System.arraycopy(dataBytes, 0, toCrypt, LONG_SIZE_IN_BYTES, dataBytes.length);


        DESKeySpec desKeySpec = new DESKeySpec(CRYPTO_PASSWORD.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(cipher);
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

        Cipher desCipher = Cipher.getInstance(cipher);
        desCipher.init(Cipher.ENCRYPT_MODE, secretKey);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CipherOutputStream cos = new CipherOutputStream(bos, desCipher);
        cos.write(toCrypt);
        cos.close();
        bos.close();

        byte[] cypherBytes = bos.toByteArray();
        return Base64.encodeBytes(cypherBytes);
    }

    /**
     * Decrypt using the given cipher the given base64-encoded, crypted data.
     * @param cipher the cypther to use.
     * @param data the base64-encoded data to decrypt.
     * @return decrypted data.
     * @throws InvalidKeyException if the given key material is shorter than 8 bytes.
     * @throws NoSuchAlgorithmException if a secret-key factory for the specified algorithm is not available in the
     *         default provider package or any of the other provider packages that were searched.
     * @throws NoSuchPaddingException if transformation contains a padding scheme that is not available.
     * @throws InvalidKeySpecException if the given key specification is inappropriate for this secret-key factory to
     *         produce a secret key.
     * @throws IOException if an I/O error occurs.
     */
    public static String decrypt(String cipher, String data) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, IOException {
        DESKeySpec desKeySpec = new DESKeySpec(CRYPTO_PASSWORD.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(cipher);
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

        Cipher desCipher = Cipher.getInstance(cipher);
        desCipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] cypherBytes = Base64.decode(data);

        ByteArrayInputStream bis = new ByteArrayInputStream(cypherBytes);
        CipherInputStream cis = new CipherInputStream(bis, desCipher);

        StringBuffer sb = new StringBuffer();

        while (true) {
            int b = cis.read();
            if (b == -1)
                break;
            sb.append((char) b);
        }

        if (sb.length() < LONG_SIZE_IN_BYTES +1)
            throw new BitronixRuntimeException("invalid crypted password '" + data + "'");

        return sb.substring(LONG_SIZE_IN_BYTES);
    }

    /**
     * Main method of this class to be used as a command-line tool to get a crypted version of a resource password.
     * @param args the command-line arguments.
     * @throws Exception when an error occurs crypting the given resource password.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Bitronix Transaction Manager password property crypter");
        System.out.flush();
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: CryptoEngine <password> [cipher]");
            System.err.println("  where:");
            System.err.println("    <password> is mandatory and is the resource password to crypt");
            System.err.println("    [cipher]   is optional and is the cipher to be used to crypt the password");
            System.exit(-1);
        }

        String data = args[0];
        String cipher = "DES";
        if (args.length > 1)
            cipher = args[1];

        String propertyValue = "{" + cipher + "}" + crypt(cipher, data);

        System.out.println("crypted password property value: " + propertyValue);
    }


    /**
     * <p>Encode and decode to / from Base64 notation.</p>
     * <p>Homepage: <a href="http://iharder.net/base64">http://iharder.net/base64</a>.</p>
     * @author Robert Harder
     * @author rob@iharder.net
     */
    private static class Base64 {

        public final static int NO_OPTIONS = 0;
        public final static int ENCODE = 1;
        public final static int DECODE = 0;
        public final static int GZIP = 2;
        public final static int DONT_BREAK_LINES = 8;
        public final static int URL_SAFE = 16;
        public final static int ORDERED = 32;

        private final static int MAX_LINE_LENGTH = 76;
        private final static byte EQUALS_SIGN = (byte) '=';
        private final static byte NEW_LINE = (byte) '\n';
        private final static String PREFERRED_ENCODING = "UTF-8";
        private final static byte WHITE_SPACE_ENC = -5; // Indicates white space in encoding
        private final static byte EQUALS_SIGN_ENC = -1; // Indicates equals sign in encoding


        /**
         * The 64 valid Base64 values.
         */
        //private final static byte[] ALPHABET;
        /* Host platform me be something funny like EBCDIC, so we hardcode these values. */
        private final static byte[] _STANDARD_ALPHABET =
                {
                        (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G',
                        (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
                        (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U',
                        (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z',
                        (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g',
                        (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n',
                        (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u',
                        (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z',
                        (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5',
                        (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '+', (byte) '/'
                };


        /**
         * Translates a Base64 value to either its 6-bit reconstruction value
         * or a negative number indicating some other meaning.
         */
        private final static byte[] _STANDARD_DECODABET =
                {
                        -9, -9, -9, -9, -9, -9, -9, -9, -9,                 // Decimal  0 -  8
                        -5, -5,                                      // Whitespace: Tab and Linefeed
                        -9, -9,                                      // Decimal 11 - 12
                        -5,                                         // Whitespace: Carriage Return
                        -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9,     // Decimal 14 - 26
                        -9, -9, -9, -9, -9,                             // Decimal 27 - 31
                        -5,                                         // Whitespace: Space
                        -9, -9, -9, -9, -9, -9, -9, -9, -9, -9,              // Decimal 33 - 42
                        62,                                         // Plus sign at decimal 43
                        -9, -9, -9,                                   // Decimal 44 - 46
                        63,                                         // Slash at decimal 47
                        52, 53, 54, 55, 56, 57, 58, 59, 60, 61,              // Numbers zero through nine
                        -9, -9, -9,                                   // Decimal 58 - 60
                        -1,                                         // Equals sign at decimal 61
                        -9, -9, -9,                                      // Decimal 62 - 64
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,            // Letters 'A' through 'N'
                        14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,        // Letters 'O' through 'Z'
                        -9, -9, -9, -9, -9, -9,                          // Decimal 91 - 96
                        26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,     // Letters 'a' through 'm'
                        39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,     // Letters 'n' through 'z'
                        -9, -9, -9, -9                                 // Decimal 123 - 126
                        /*,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 127 - 139
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255 */
                };


        /**
         * Used in the URL- and Filename-safe dialect described in Section 4 of RFC3548:
         * <a href="http://www.faqs.org/rfcs/rfc3548.html">http://www.faqs.org/rfcs/rfc3548.html</a>.
         * Notice that the last two bytes become "hyphen" and "underscore" instead of "plus" and "slash."
         */
        private final static byte[] _URL_SAFE_ALPHABET =
                {
                        (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G',
                        (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
                        (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U',
                        (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z',
                        (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g',
                        (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n',
                        (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u',
                        (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z',
                        (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5',
                        (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '-', (byte) '_'
                };

        /**
         * Used in decoding URL- and Filename-safe dialects of Base64.
         */
        private final static byte[] _URL_SAFE_DECODABET =
                {
                        -9, -9, -9, -9, -9, -9, -9, -9, -9,                 // Decimal  0 -  8
                        -5, -5,                                      // Whitespace: Tab and Linefeed
                        -9, -9,                                      // Decimal 11 - 12
                        -5,                                         // Whitespace: Carriage Return
                        -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9,     // Decimal 14 - 26
                        -9, -9, -9, -9, -9,                             // Decimal 27 - 31
                        -5,                                         // Whitespace: Space
                        -9, -9, -9, -9, -9, -9, -9, -9, -9, -9,              // Decimal 33 - 42
                        -9,                                         // Plus sign at decimal 43
                        -9,                                         // Decimal 44
                        62,                                         // Minus sign at decimal 45
                        -9,                                         // Decimal 46
                        -9,                                         // Slash at decimal 47
                        52, 53, 54, 55, 56, 57, 58, 59, 60, 61,              // Numbers zero through nine
                        -9, -9, -9,                                   // Decimal 58 - 60
                        -1,                                         // Equals sign at decimal 61
                        -9, -9, -9,                                   // Decimal 62 - 64
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,            // Letters 'A' through 'N'
                        14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,        // Letters 'O' through 'Z'
                        -9, -9, -9, -9,                                // Decimal 91 - 94
                        63,                                         // Underscore at decimal 95
                        -9,                                         // Decimal 96
                        26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,     // Letters 'a' through 'm'
                        39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,     // Letters 'n' through 'z'
                        -9, -9, -9, -9                                 // Decimal 123 - 126
                        /*,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 127 - 139
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255 */
                };

        /**
         * I don't get the point of this technique, but it is described here:
         * <a href="http://www.faqs.org/qa/rfcc-1940.html">http://www.faqs.org/qa/rfcc-1940.html</a>.
         */
        private final static byte[] _ORDERED_ALPHABET =
                {
                        (byte) '-',
                        (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4',
                        (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9',
                        (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G',
                        (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
                        (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U',
                        (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z',
                        (byte) '_',
                        (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g',
                        (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n',
                        (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u',
                        (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z'
                };

        /**
         * Used in decoding the "ordered" dialect of Base64.
         */
        private final static byte[] _ORDERED_DECODABET =
                {
                        -9, -9, -9, -9, -9, -9, -9, -9, -9,                 // Decimal  0 -  8
                        -5, -5,                                      // Whitespace: Tab and Linefeed
                        -9, -9,                                      // Decimal 11 - 12
                        -5,                                         // Whitespace: Carriage Return
                        -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9,     // Decimal 14 - 26
                        -9, -9, -9, -9, -9,                             // Decimal 27 - 31
                        -5,                                         // Whitespace: Space
                        -9, -9, -9, -9, -9, -9, -9, -9, -9, -9,              // Decimal 33 - 42
                        -9,                                         // Plus sign at decimal 43
                        -9,                                         // Decimal 44
                        0,                                          // Minus sign at decimal 45
                        -9,                                         // Decimal 46
                        -9,                                         // Slash at decimal 47
                        1, 2, 3, 4, 5, 6, 7, 8, 9, 10,                       // Numbers zero through nine
                        -9, -9, -9,                                   // Decimal 58 - 60
                        -1,                                         // Equals sign at decimal 61
                        -9, -9, -9,                                   // Decimal 62 - 64
                        11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,     // Letters 'A' through 'M'
                        24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,     // Letters 'N' through 'Z'
                        -9, -9, -9, -9,                                // Decimal 91 - 94
                        37,                                         // Underscore at decimal 95
                        -9,                                         // Decimal 96
                        38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,     // Letters 'a' through 'm'
                        51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,     // Letters 'n' through 'z'
                        -9, -9, -9, -9                                 // Decimal 123 - 126
                        /*,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 127 - 139
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
          -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255 */
                };

        /**
         * Returns one of the _SOMETHING_ALPHABET byte arrays depending on
         * the options specified.
         * It's possible, though silly, to specify ORDERED and URLSAFE
         * in which case one of them will be picked, though there is
         * no guarantee as to which one will be picked.
         * @param options the options
         * @return a byte array
         */
        private static byte[] getAlphabet(int options) {
            if ((options & URL_SAFE) == URL_SAFE) return _URL_SAFE_ALPHABET;
            else if ((options & ORDERED) == ORDERED) return _ORDERED_ALPHABET;
            else return _STANDARD_ALPHABET;

        }    // end getAlphabet


        /**
         * Returns one of the _SOMETHING_DECODABET byte arrays depending on
         * the options specified.
         * It's possible, though silly, to specify ORDERED and URL_SAFE
         * in which case one of them will be picked, though there is
         * no guarantee as to which one will be picked.
         * @param options the options
         * @return a byte array
         */
        private static byte[] getDecodabet(int options) {
            if ((options & URL_SAFE) == URL_SAFE) return _URL_SAFE_DECODABET;
            else if ((options & ORDERED) == ORDERED) return _ORDERED_DECODABET;
            else return _STANDARD_DECODABET;

        }    // end getAlphabet


        /**
         * Defeats instantiation.
         */
        private Base64() {
        }


        /**
         * Encodes up to the first three bytes of array <var>threeBytes</var>
         * and returns a four-byte array in Base64 notation.
         * The actual number of significant bytes in your array is
         * given by <var>numSigBytes</var>.
         * The array <var>threeBytes</var> needs only be as big as
         * <var>numSigBytes</var>.
         * Code can reuse a byte array by passing a four-byte array as <var>b4</var>.
         *
         * @param b4          A reusable byte array to reduce array instantiation
         * @param threeBytes  the array to convert
         * @param numSigBytes the number of significant bytes in your array
         * @param options the options
         * @return four byte array in Base64 notation.
         * @since 1.5.1
         */
        private static byte[] encode3to4(byte[] b4, byte[] threeBytes, int numSigBytes, int options) {
            encode3to4(threeBytes, 0, numSigBytes, b4, 0, options);
            return b4;
        }   // end encode3to4


        /**
         * <p>Encodes up to three bytes of the array <var>source</var>
         * and writes the resulting four Base64 bytes to <var>destination</var>.
         * The source and destination arrays can be manipulated
         * anywhere along their length by specifying
         * <var>srcOffset</var> and <var>destOffset</var>.
         * This method does not check to make sure your arrays
         * are large enough to accomodate <var>srcOffset</var> + 3 for
         * the <var>source</var> array or <var>destOffset</var> + 4 for
         * the <var>destination</var> array.
         * The actual number of significant bytes in your array is
         * given by <var>numSigBytes</var>.</p>
         * <p>This is the lowest level of the encoding methods with
         * all possible parameters.</p>
         *
         * @param source      the array to convert
         * @param srcOffset   the index where conversion begins
         * @param numSigBytes the number of significant bytes in your array
         * @param destination the array to hold the conversion
         * @param destOffset  the index where output will be put
         * @param options the options
         * @return the <var>destination</var> array
         * @since 1.3
         */
        private static byte[] encode3to4(
                byte[] source, int srcOffset, int numSigBytes,
                byte[] destination, int destOffset, int options) {
            byte[] ALPHABET = getAlphabet(options);

            //           1         2         3
            // 01234567890123456789012345678901 Bit position
            // --------000000001111111122222222 Array position from threeBytes
            // --------|    ||    ||    ||    | Six bit groups to index ALPHABET
            //          >>18  >>12  >> 6  >> 0  Right shift necessary
            //                0x3f  0x3f  0x3f  Additional AND

            // Create buffer with zero-padding if there are only one or two
            // significant bytes passed in the array.
            // We have to shift left 24 in order to flush out the 1's that appear
            // when Java treats a value as negative that is cast from a byte to an int.
            int inBuff = (numSigBytes > 0 ? ((source[srcOffset] << 24) >>> 8) : 0)
                    | (numSigBytes > 1 ? ((source[srcOffset + 1] << 24) >>> 16) : 0)
                    | (numSigBytes > 2 ? ((source[srcOffset + 2] << 24) >>> 24) : 0);

            switch (numSigBytes) {
                case 3:
                    destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                    destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                    destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
                    destination[destOffset + 3] = ALPHABET[(inBuff) & 0x3f];
                    return destination;

                case 2:
                    destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                    destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                    destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
                    destination[destOffset + 3] = EQUALS_SIGN;
                    return destination;

                case 1:
                    destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                    destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                    destination[destOffset + 2] = EQUALS_SIGN;
                    destination[destOffset + 3] = EQUALS_SIGN;
                    return destination;

                default:
                    return destination;
            }   // end switch
        }   // end encode3to4

        /**
         * Encodes a byte array into Base64 notation.
         * Does not GZip-compress data.
         *
         * @param source The data to convert
         * @return a String
         * @since 1.4
         */
        public static String encodeBytes(byte[] source) {
            return encodeBytes(source, 0, source.length, NO_OPTIONS);
        }   // end encodeBytes

        /**
         * Encodes a byte array into Base64 notation.
         * <p/>
         * Valid options:<pre>
         *   GZIP: gzip-compresses object before encoding it.
         *   DONT_BREAK_LINES: don't break lines at 76 characters
         *     <i>Note: Technically, this makes your encoding non-compliant.</i>
         * </pre>
         * <p/>
         * Example: <code>encodeBytes( myData, Base64.GZIP )</code> or
         * <p/>
         * Example: <code>encodeBytes( myData, Base64.GZIP | Base64.DONT_BREAK_LINES )</code>
         *
         * @param source  The data to convert
         * @param off     Offset in array where conversion should begin
         * @param len     Length of data to convert
         * @param options alphabet type is pulled from this (standard, url-safe, ordered)
         * @see Base64#GZIP
         * @see Base64#DONT_BREAK_LINES
         * @since 2.0
         * @return a String
         */
        public static String encodeBytes(byte[] source, int off, int len, int options) {
            // Isolate options
            int dontBreakLines = (options & DONT_BREAK_LINES);
            int gzip = (options & GZIP);

            // Compress?
            if (gzip == GZIP) {
                java.io.ByteArrayOutputStream baos = null;
                java.util.zip.GZIPOutputStream gzos = null;
                Base64.OutputStream b64os = null;


                try {
                    // GZip -> Base64 -> ByteArray
                    baos = new java.io.ByteArrayOutputStream();
                    b64os = new Base64.OutputStream(baos, ENCODE | options);
                    gzos = new java.util.zip.GZIPOutputStream(b64os);

                    gzos.write(source, off, len);
                    gzos.close();
                }   // end try
                catch (java.io.IOException e) {
                    e.printStackTrace();
                    return null;
                }   // end catch
                finally {
                    try {
                        gzos.close();
                    } catch (Exception e) {
                        // ignore
                    }
                    try {
                        b64os.close();
                    } catch (Exception e) {
                        // ignore
                    }
                    try {
                        baos.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }   // end finally

                // Return value according to relevant encoding.
                try {
                    return new String(baos.toByteArray(), PREFERRED_ENCODING);
                }   // end try
                catch (java.io.UnsupportedEncodingException uue) {
                    return new String(baos.toByteArray());
                }   // end catch
            }   // end if: compress

            // Else, don't compress. Better not to use streams at all then.
            else {
                // Convert option to boolean in way that code likes it.
                boolean breakLines = dontBreakLines == 0;

                int len43 = len * 4 / 3;
                byte[] outBuff = new byte[(len43)                      // Main 4:3
                        + ((len % 3) > 0 ? 4 : 0)      // Account for padding
                        + (breakLines ? (len43 / MAX_LINE_LENGTH) : 0)]; // New lines
                int d = 0;
                int e = 0;
                int len2 = len - 2;
                int lineLength = 0;
                for (; d < len2; d += 3, e += 4) {
                    encode3to4(source, d + off, 3, outBuff, e, options);

                    lineLength += 4;
                    if (breakLines && lineLength == MAX_LINE_LENGTH) {
                        outBuff[e + 4] = NEW_LINE;
                        e++;
                        lineLength = 0;
                    }   // end if: end of line
                }   // en dfor: each piece of array

                if (d < len) {
                    encode3to4(source, d + off, len - d, outBuff, e, options);
                    e += 4;
                }   // end if: some padding needed

                // Return value according to relevant encoding.
                try {
                    return new String(outBuff, 0, e, PREFERRED_ENCODING);
                }   // end try
                catch (java.io.UnsupportedEncodingException uue) {
                    return new String(outBuff, 0, e);
                }   // end catch

            }   // end else: don't compress

        }   // end encodeBytes

        /**
         * Decodes four bytes from array <var>source</var>
         * and writes the resulting bytes (up to three of them)
         * to <var>destination</var>.
         * The source and destination arrays can be manipulated
         * anywhere along their length by specifying
         * <var>srcOffset</var> and <var>destOffset</var>.
         * This method does not check to make sure your arrays
         * are large enough to accomodate <var>srcOffset</var> + 4 for
         * the <var>source</var> array or <var>destOffset</var> + 3 for
         * the <var>destination</var> array.
         * This method returns the actual number of bytes that
         * were converted from the Base64 encoding.
         * <p>This is the lowest level of the decoding methods with
         * all possible parameters.</p>
         *
         * @param source      the array to convert
         * @param srcOffset   the index where conversion begins
         * @param destination the array to hold the conversion
         * @param destOffset  the index where output will be put
         * @param options     alphabet type is pulled from this (standard, url-safe, ordered)
         * @return the number of decoded bytes converted
         * @since 1.3
         */
        private static int decode4to3(byte[] source, int srcOffset, byte[] destination, int destOffset, int options) {
            byte[] DECODABET = getDecodabet(options);

            // Example: Dk==
            if (source[srcOffset + 2] == EQUALS_SIGN) {
                // Two ways to do the same thing. Don't know which way I like best.
                //int outBuff =   ( ( DECODABET[ source[ srcOffset    ] ] << 24 ) >>>  6 )
                //              | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );
                int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
                        | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12);

                destination[destOffset] = (byte) (outBuff >>> 16);
                return 1;
            }

            // Example: DkL=
            else if (source[srcOffset + 3] == EQUALS_SIGN) {
                // Two ways to do the same thing. Don't know which way I like best.
                //int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] << 24 ) >>>  6 )
                //              | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
                //              | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );
                int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
                        | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12)
                        | ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6);

                destination[destOffset] = (byte) (outBuff >>> 16);
                destination[destOffset + 1] = (byte) (outBuff >>> 8);
                return 2;
            }

            // Example: DkLE
            else {
                try {
                    // Two ways to do the same thing. Don't know which way I like best.
                    //int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] << 24 ) >>>  6 )
                    //              | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
                    //              | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )
                    //              | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );
                    int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
                            | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12)
                            | ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6)
                            | ((DECODABET[source[srcOffset + 3]] & 0xFF));


                    destination[destOffset] = (byte) (outBuff >> 16);
                    destination[destOffset + 1] = (byte) (outBuff >> 8);
                    destination[destOffset + 2] = (byte) (outBuff);

                    return 3;
                } catch (Exception e) {
                    System.out.println("" + source[srcOffset] + ": " + (DECODABET[source[srcOffset]]));
                    System.out.println("" + source[srcOffset + 1] + ": " + (DECODABET[source[srcOffset + 1]]));
                    System.out.println("" + source[srcOffset + 2] + ": " + (DECODABET[source[srcOffset + 2]]));
                    System.out.println("" + source[srcOffset + 3] + ": " + (DECODABET[source[srcOffset + 3]]));
                    return -1;
                }   // end catch
            }
        }   // end decodeToBytes


        /**
         * Very low-level access to decoding ASCII characters in
         * the form of a byte array. Does not support automatically
         * gunzipping or any other "fancy" features.
         *
         * @param source The Base64 encoded data
         * @param off    The offset of where to begin decoding
         * @param len    The length of characters to decode
         * @param options the options
         * @return decoded data
         * @since 1.3
         */
        public static byte[] decode(byte[] source, int off, int len, int options) {
            byte[] DECODABET = getDecodabet(options);

            int len34 = len * 3 / 4;
            byte[] outBuff = new byte[len34]; // Upper limit on size of output
            int outBuffPosn = 0;

            byte[] b4 = new byte[4];
            int b4Posn = 0;
            int i;
            byte sbiCrop;
            byte sbiDecode;
            for (i = off; i < off + len; i++) {
                sbiCrop = (byte) (source[i] & 0x7f); // Only the low seven bits
                sbiDecode = DECODABET[sbiCrop];

                if (sbiDecode >= WHITE_SPACE_ENC) // White space, Equals sign or better
                {
                    if (sbiDecode >= EQUALS_SIGN_ENC) {
                        b4[b4Posn++] = sbiCrop;
                        if (b4Posn > 3) {
                            outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn, options);
                            b4Posn = 0;

                            // If that was the equals sign, break out of 'for' loop
                            if (sbiCrop == EQUALS_SIGN)
                                break;
                        }   // end if: quartet built

                    }   // end if: equals sign or better

                }   // end if: white space, equals sign or better
                else {
                    System.err.println("Bad Base64 input character at " + i + ": " + source[i] + "(decimal)");
                    return null;
                }   // end else:
            }   // each input character

            byte[] out = new byte[outBuffPosn];
            System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
            return out;
        }   // end decode


        /**
         * Decodes data from Base64 notation, automatically
         * detecting gzip-compressed data and decompressing it.
         *
         * @param s the string to decode
         * @return the decoded data
         * @since 1.4
         */
        public static byte[] decode(String s) {
            return decode(s, NO_OPTIONS);
        }


        /**
         * Decodes data from Base64 notation, automatically
         * detecting gzip-compressed data and decompressing it.
         *
         * @param s       the string to decode
         * @param options encode options such as URL_SAFE
         * @return the decoded data
         * @since 1.4
         */
        public static byte[] decode(String s, int options) {
            byte[] bytes;
            try {
                bytes = s.getBytes(PREFERRED_ENCODING);
            }   // end try
            catch (java.io.UnsupportedEncodingException uee) {
                bytes = s.getBytes();
            }   // end catch
            //</change>

            // Decode
            bytes = decode(bytes, 0, bytes.length, options);

            // Check to see if it's gzip-compressed
            // GZIP Magic Two-Byte Number: 0x8b1f (35615)
            if (bytes != null && bytes.length >= 4) {

                int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
                if (java.util.zip.GZIPInputStream.GZIP_MAGIC == head) {
                    java.io.ByteArrayInputStream bais = null;
                    java.util.zip.GZIPInputStream gzis = null;
                    java.io.ByteArrayOutputStream baos = null;
                    byte[] buffer = new byte[2048];
                    int length;

                    try {
                        baos = new java.io.ByteArrayOutputStream();
                        bais = new java.io.ByteArrayInputStream(bytes);
                        gzis = new java.util.zip.GZIPInputStream(bais);

                        while ((length = gzis.read(buffer)) >= 0) {
                            baos.write(buffer, 0, length);
                        }   // end while: reading input

                        // No error? Get new bytes.
                        bytes = baos.toByteArray();

                    }   // end try
                    catch (java.io.IOException e) {
                        // Just return originally-decoded bytes
                    }   // end catch
                    finally {
                        try {
                            baos.close();
                        } catch (Exception e) {
                            // ignore
                        }
                        try {
                            gzis.close();
                        } catch (Exception e) {
                            // ignore
                        }
                        try {
                            bais.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }   // end finally

                }   // end if: gzipped
            }   // end if: bytes.length >= 2

            return bytes;
        }   // end decode


        /**
         * Attempts to decode Base64 data and deserialize a Java
         * Object within. Returns <tt>null</tt> if there was an error.
         *
         * @param encodedObject The Base64 data to decode
         * @return The decoded and deserialized object
         * @since 1.5
         */
        public static Object decodeToObject(String encodedObject) {
            // Decode and gunzip if necessary
            byte[] objBytes = decode(encodedObject);

            java.io.ByteArrayInputStream bais = null;
            java.io.ObjectInputStream ois = null;
            Object obj = null;

            try {
                bais = new java.io.ByteArrayInputStream(objBytes);
                ois = new java.io.ObjectInputStream(bais);

                obj = ois.readObject();
            }   // end try
            catch (java.io.IOException e) {
                e.printStackTrace();
                obj = null;
            }   // end catch
            catch (java.lang.ClassNotFoundException e) {
                e.printStackTrace();
                obj = null;
            }   // end catch
            finally {
                try {
                    if (bais != null) bais.close();
                } catch (Exception e) {
                    // ignore
                }
                try {
                    if (ois != null) ois.close();
                } catch (Exception e) {
                    // ignore
                }
            }   // end finally

            return obj;
        }   // end decodeObject

        /**
         * A {@link Base64.OutputStream} will write data to another
         * <tt>java.io.OutputStream</tt>, given in the constructor,
         * and encode/decode to/from Base64 notation on the fly.
         *
         * @see Base64
         * @since 1.3
         */
        public static class OutputStream extends java.io.FilterOutputStream {
            private boolean encode;
            private int position;
            private byte[] buffer;
            private int bufferLength;
            private int lineLength;
            private boolean breakLines;
            private byte[] b4; // Scratch used in a few places
            private boolean suspendEncoding;
            private int options; // Record for later
            private byte[] decodabet;        // Local copies to avoid extra method calls

            /**
             * Constructs a {@link Base64.OutputStream} in ENCODE mode.
             *
             * @param out the <tt>java.io.OutputStream</tt> to which data will be written.
             * @since 1.3
             */
            public OutputStream(java.io.OutputStream out) {
                this(out, ENCODE);
            }   // end constructor


            /**
             * Constructs a {@link Base64.OutputStream} in
             * either ENCODE or DECODE mode.
             * <p/>
             * Valid options:<pre>
             *   ENCODE or DECODE: Encode or Decode as data is read.
             *   DONT_BREAK_LINES: don't break lines at 76 characters
             *     (only meaningful when encoding)
             *     <i>Note: Technically, this makes your encoding non-compliant.</i>
             * </pre>
             * <p/>
             * Example: <code>new Base64.OutputStream( out, Base64.ENCODE )</code>
             *
             * @param out     the <tt>java.io.OutputStream</tt> to which data will be written.
             * @param options Specified options.
             * @see Base64#ENCODE
             * @see Base64#DECODE
             * @see Base64#DONT_BREAK_LINES
             * @since 1.3
             */
            public OutputStream(java.io.OutputStream out, int options) {
                super(out);
                this.breakLines = (options & DONT_BREAK_LINES) != DONT_BREAK_LINES;
                this.encode = (options & ENCODE) == ENCODE;
                this.bufferLength = encode ? 3 : 4;
                this.buffer = new byte[bufferLength];
                this.position = 0;
                this.lineLength = 0;
                this.suspendEncoding = false;
                this.b4 = new byte[4];
                this.options = options;
                this.decodabet = getDecodabet(options);
            }   // end constructor


            /**
             * Writes the byte to the output stream after
             * converting to/from Base64 notation.
             * When encoding, bytes are buffered three
             * at a time before the output stream actually
             * gets a write() call.
             * When decoding, bytes are buffered four
             * at a time.
             *
             * @param theByte the byte to write
             * @since 1.3
             */
            public void write(int theByte) throws java.io.IOException {
                // Encoding suspended?
                if (suspendEncoding) {
                    super.out.write(theByte);
                    return;
                }   // end if: supsended

                // Encode?
                if (encode) {
                    buffer[position++] = (byte) theByte;
                    if (position >= bufferLength)  // Enough to encode.
                    {
                        out.write(encode3to4(b4, buffer, bufferLength, options));

                        lineLength += 4;
                        if (breakLines && lineLength >= MAX_LINE_LENGTH) {
                            out.write(NEW_LINE);
                            lineLength = 0;
                        }   // end if: end of line

                        position = 0;
                    }   // end if: enough to output
                }   // end if: encoding

                // Else, Decoding
                else {
                    // Meaningful Base64 character?
                    if (decodabet[theByte & 0x7f] > WHITE_SPACE_ENC) {
                        buffer[position++] = (byte) theByte;
                        if (position >= bufferLength)  // Enough to output.
                        {
                            int len = Base64.decode4to3(buffer, 0, b4, 0, options);
                            out.write(b4, 0, len);
                            //out.write( Base64.decode4to3( buffer ) );
                            position = 0;
                        }   // end if: enough to output
                    }   // end if: meaningful base64 character
                    else if (decodabet[theByte & 0x7f] != WHITE_SPACE_ENC) {
                        throw new java.io.IOException("Invalid character in Base64 data.");
                    }   // end else: not white space either
                }   // end else: decoding
            }   // end write


            /**
             * Calls {@link #write(int)} repeatedly until <var>len</var>
             * bytes are written.
             *
             * @param theBytes array from which to read bytes
             * @param off      offset for array
             * @param len      max number of bytes to read into array
             * @since 1.3
             */
            public void write(byte[] theBytes, int off, int len) throws java.io.IOException {
                // Encoding suspended?
                if (suspendEncoding) {
                    super.out.write(theBytes, off, len);
                    return;
                }   // end if: supsended

                for (int i = 0; i < len; i++) {
                    write(theBytes[off + i]);
                }   // end for: each byte written

            }   // end write


            /**
             * Method added by PHIL. [Thanks, PHIL. -Rob]
             * This pads the buffer without closing the stream.
             * @throws java.io.IOException when wrong padding is used
             */
            public void flushBase64() throws java.io.IOException {
                if (position > 0) {
                    if (encode) {
                        out.write(encode3to4(b4, buffer, position, options));
                        position = 0;
                    }   // end if: encoding
                    else {
                        throw new java.io.IOException("Base64 input not properly padded.");
                    }   // end else: decoding
                }   // end if: buffer partially full

            }   // end flush


            /**
             * Flushes and closes (I think, in the superclass) the stream.
             *
             * @since 1.3
             */
            public void close() throws java.io.IOException {
                // 1. Ensure that pending characters are written
                flushBase64();

                // 2. Actually close the stream
                // Base class both flushes and closes.
                super.close();

                buffer = null;
                out = null;
            }   // end close


            /**
             * Suspends encoding of the stream.
             * May be helpful if you need to embed a piece of
             * base640-encoded data in a stream.
             *
             * @since 1.5.1
             * @throws java.io.IOException never thrown
             */
            public void suspendEncoding() throws java.io.IOException {
                flushBase64();
                this.suspendEncoding = true;
            }   // end suspendEncoding


            /**
             * Resumes encoding of the stream.
             * May be helpful if you need to embed a piece of
             * base640-encoded data in a stream.
             *
             * @since 1.5.1
             */
            public void resumeEncoding() {
                this.suspendEncoding = false;
            }   // end resumeEncoding

        }   // end inner class OutputStream
    }   // end class Base64

}
