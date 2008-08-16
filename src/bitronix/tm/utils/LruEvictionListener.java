package bitronix.tm.utils;

/**
 * Eviction listener interface for {@link LruMap}.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public interface LruEvictionListener {

    public void onEviction(Object value);

}
