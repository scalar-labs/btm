package bitronix.tm.internal;

/**
 * <p>Simple crypto helper that uses symetric keys to crypt and decrypt resources passwords.</p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @deprecated superceded by {@link bitronix.tm.utils.CryptoEngine}.
 * @author lorban
 */
public class CryptoEngine {

    public static void main(String[] args) throws Exception {
        System.out.println("WARNING: bitronix.tm.internal.CryptoEngine has been replaced by bitronix.tm.utils.CryptoEngine.");
        bitronix.tm.utils.CryptoEngine.main(args);
    }

}
