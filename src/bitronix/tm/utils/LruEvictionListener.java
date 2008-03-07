package bitronix.tm.utils;

/**
 * Eviction listener interface for {@link LruMap}.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public interface LruEvictionListener {

    public void onEviction(Object value);

}
