package bitronix.tm.utils;

import java.util.Collection;
import java.util.Iterator;

/**
 * <p>{@link Collection} helper functions.</p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class CollectionUtils {

    /**
     * Check if a collection contains a specific object by searching for it by identity
     * instead of by using equals/hashcode.
     * @param collection the collection to search in.
     * @param toBeFound the object to search for.
     * @return true if the collection contains the object, false otherwise.
     */
    public static boolean containsByIdentity(Collection collection, Object toBeFound) {
        Iterator it = collection.iterator();
        while (it.hasNext()) {
            Object o =  it.next();
            if (o == toBeFound)
                return true;
        }
        return false;
    }

}
