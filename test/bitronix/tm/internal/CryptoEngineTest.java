package bitronix.tm.internal;

import junit.framework.TestCase;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class CryptoEngineTest extends TestCase {

    public void testCrypt() throws Exception {
    	String textToCrypt = "java";

    	String cypherText = CryptoEngine.crypt("DES", textToCrypt);
        String decryptedText = CryptoEngine.decrypt("DES", cypherText);

        assertEquals(textToCrypt, decryptedText);
    }

}
