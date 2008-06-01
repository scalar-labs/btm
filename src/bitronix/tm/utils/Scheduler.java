package bitronix.tm.utils;

import java.util.*;

/**
 * <p>&copy; Bitronix 2005, 2006, 2007, 2008</p>
 *
 * @author lorban
 */
public class Scheduler {

    public static final int DEFAULT_POSITION = 0;
    public static final int ALWAYS_FIRST_POSITION = Integer.MIN_VALUE;
    public static final int ALWAYS_LAST_POSITION = Integer.MAX_VALUE;

    public static final Object DEFAULT_POSITION_KEY = new Integer(DEFAULT_POSITION);
    public static final Object ALWAYS_FIRST_POSITION_KEY = new Integer(ALWAYS_FIRST_POSITION);
    public static final Object ALWAYS_LAST_POSITION_KEY = new Integer(ALWAYS_LAST_POSITION);

    private Map objects = new TreeMap();

    
    public Scheduler() {
    }

    public void add(Object obj, int position) {
        Integer key = new Integer(position);
        List synchronizationsList = (List) objects.get(key);
        if (synchronizationsList == null) {
            synchronizationsList = new ArrayList();
            objects.put(key, synchronizationsList);
        }
        synchronizationsList.add(obj);
    }

    public void remove(Object obj) {
        Iterator it = iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o == obj) {
                it.remove();
                return;
            }
        }
        throw new NoSuchElementException("no such element: " + obj);
    }

    public SortedSet getNaturalOrderPositions() {
        return new TreeSet(objects.keySet());
    }

    public List getNaturalOrderSynchronizationsForPosition(Object positionKey) {
        return (List) objects.get(positionKey);
    }

    public List getNaturalOrderResourcesForPosition(Object positionKey) {
        return (List) objects.get(positionKey);
    }

    public List getReverseOrderResourcesForPosition(Object positionKey) {
        List result = new ArrayList(getNaturalOrderResourcesForPosition(positionKey));
        Collections.reverse(result);
        return result;
    }

    public SortedSet getReverseOrderPositions() {
        TreeSet result = new TreeSet(Collections.reverseOrder());
        result.addAll(getNaturalOrderPositions());
        return result;
    }

    public int size() {
        int totalSize = 0;
        Iterator it = objects.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            List list = (List) objects.get(key);
            totalSize += list.size();
        }
        return totalSize;
    }

    public Iterator iterator() {
        return new SchedulerIterator(this);
    }

    public String toString() {
        return "a Scheduler with " + size() + " object(s) in " + getNaturalOrderPositions().size() + " position(s)";
    }

    private class SchedulerIterator implements Iterator {
        private Scheduler scheduler;
        private Iterator schedulerKeysIterator;
        private List objectsOfCurrentKey;
        private int objectsOfCurrentKeyIndex;

        private SchedulerIterator(Scheduler scheduler) {
            this.scheduler = scheduler;
            schedulerKeysIterator = scheduler.objects.keySet().iterator();
        }

        public void remove() {
            if (objectsOfCurrentKey == null)
                throw new NoSuchElementException("iterator not yet placed on an element");

            objectsOfCurrentKeyIndex--;
            objectsOfCurrentKey.remove(objectsOfCurrentKeyIndex);
            if (objectsOfCurrentKey.size() == 0) {
                schedulerKeysIterator.remove();
                objectsOfCurrentKey = null;
            }
        }

        public boolean hasNext() {
            if (objectsOfCurrentKey == null || objectsOfCurrentKeyIndex >= objectsOfCurrentKey.size()) {
                if (schedulerKeysIterator.hasNext()) {
                    Integer currentKey = (Integer) schedulerKeysIterator.next();
                    objectsOfCurrentKey = (List) scheduler.objects.get(currentKey);
                    objectsOfCurrentKeyIndex = 0;
                    return true;
                }
                return false;
            }
            return true;
        }

        public Object next() {
            if (!hasNext())
                throw new NoSuchElementException("iterator bounds reached");
            return objectsOfCurrentKey.get(objectsOfCurrentKeyIndex++);
        }
    }


}
