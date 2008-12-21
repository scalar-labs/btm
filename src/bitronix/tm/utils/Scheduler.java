package bitronix.tm.utils;

import java.util.*;

/**
 * Positional object container. Objects can be added to a scheduler at a certain position (or priority) and can be
 * retrieved later on in their position + added order. All the objects of a scheduler can be iterated in order or
 * objects of a cetain position can be retrieved for iteration.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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

    private List keys = new ArrayList();
    private Map objects = new TreeMap();
    private int size = 0;


    public Scheduler() {
    }

    public synchronized void add(Object obj, int position) {
        Integer key = new Integer(position);
        List synchronizationsList = (List) objects.get(key);
        if (synchronizationsList == null) {
            if (!keys.contains(key)) {
                keys.add(key);
                Collections.sort(keys);
            }
            synchronizationsList = new ArrayList();
            objects.put(key, synchronizationsList);
        }
        synchronizationsList.add(obj);
        size++;
    }

    public synchronized void remove(Object obj) {
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

    public synchronized SortedSet getNaturalOrderPositions() {
        return new TreeSet(objects.keySet());
    }

    public synchronized SortedSet getReverseOrderPositions() {
        TreeSet result = new TreeSet(Collections.reverseOrder());
        result.addAll(getNaturalOrderPositions());
        return result;
    }

    public synchronized List getByNaturalOrderForPosition(Object positionKey) {
        return (List) objects.get(positionKey);
    }

    public synchronized List getByReverseOrderForPosition(Object positionKey) {
        List result = new ArrayList(getByNaturalOrderForPosition(positionKey));
        Collections.reverse(result);
        return result;
    }

    public synchronized int size() {
        return size;
    }

    public Iterator iterator() {
        return new SchedulerIterator(this);
    }

    public String toString() {
        return "a Scheduler with " + size() + " object(s) in " + getNaturalOrderPositions().size() + " position(s)";
    }

    private class SchedulerIterator implements Iterator {
        private Scheduler scheduler;
        private int nextKeyIndex;
        private List objectsOfCurrentKey;
        private int objectsOfCurrentKeyIndex;

        private SchedulerIterator(Scheduler scheduler) {
            this.scheduler = scheduler;
            this.nextKeyIndex = 0;
        }

        public void remove() {
            if (objectsOfCurrentKey == null)
                throw new NoSuchElementException("iterator not yet placed on an element");

            objectsOfCurrentKeyIndex--;
            objectsOfCurrentKey.remove(objectsOfCurrentKeyIndex);
            if (objectsOfCurrentKey.size() == 0) {
                // there are no more objects in the current position's list -> remove it
                nextKeyIndex--;
                Object key = scheduler.keys.get(nextKeyIndex);
                scheduler.keys.remove(nextKeyIndex);
                scheduler.objects.remove(key);
                objectsOfCurrentKey = null;
            }
            scheduler.size--;
        }

        public boolean hasNext() {
            if (objectsOfCurrentKey == null || objectsOfCurrentKeyIndex >= objectsOfCurrentKey.size()) {
                // we reached the end of the current position's list

                if (nextKeyIndex < scheduler.keys.size()) {
                    // there is another position after this one
                    Integer currentKey = (Integer) scheduler.keys.get(nextKeyIndex++);
                    objectsOfCurrentKey = (List) scheduler.objects.get(currentKey);
                    objectsOfCurrentKeyIndex = 0;
                    return true;
                }
                else {
                    // there is no other position after this one
                    return false;
                }
                
            }

            // there are still objects in the current position's list
            return true;
        }

        public Object next() {
            if (!hasNext())
                throw new NoSuchElementException("iterator bounds reached");
            return objectsOfCurrentKey.get(objectsOfCurrentKeyIndex++);
        }
    }

}
