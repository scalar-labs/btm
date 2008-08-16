package bitronix.tm.utils;

import java.util.*;

/**
 * Last Recently Used Map with eviction listeners support implementation.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class LruMap extends HashMap {

    private int maxSize;
    private Map usageMap = new HashMap();
    private List evictionListners = new ArrayList();

    public LruMap(int maxSize) {
        this.maxSize = maxSize;
    }

    public Object put(Object key, Object value) {
        if (maxSize < 1) {
            return null;
        }
        
        if (size() >= maxSize)
            evictOne();
        
        usageMap.put(key, new Integer(0));
        return super.put(key, value);
    }

    public Object get(Object key) {
        incrementUsage(key);
        return super.get(key);
    }

    public Object remove(Object key) {
        usageMap.remove(key);
        return super.remove(key);
    }

    public void clear() {
        usageMap.clear();
        super.clear();
    }

    private void incrementUsage(Object key) {
        Integer value = (Integer) usageMap.get(key);
        if (value == null)
            return;

        Integer newValue = new Integer(value.intValue() + 1);
        usageMap.put(key, newValue);
    }

    private void evictOne() {
        int lowestValue = 0;
        Object lruKey = null;

        Iterator it = usageMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Object key = entry.getKey();
            Integer value = (Integer) entry.getValue();

            if (lruKey == null || value.intValue() < lowestValue) {
                lowestValue = value.intValue();
                lruKey = key;
            }
        }

        if (lruKey != null) {
            usageMap.remove(lruKey);
            Object value = remove(lruKey);
            fireEvictionEvent(value);
        }
    }

    private void fireEvictionEvent(Object value) {
        for (int i = 0; i < evictionListners.size(); i++) {
            LruEvictionListener listener = (LruEvictionListener) evictionListners.get(i);
            listener.onEviction(value);
        }
    }

    public void addEvictionListener(LruEvictionListener listener) {
        evictionListners.add(listener);
    }

    public void removeEvictionListener(LruEvictionListener listener) {
        evictionListners.remove(listener);
    }

}
