/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Positional object container. Objects can be added to a scheduler at a certain position (or priority) and can be
 * retrieved later on in their position + added order. All the objects of a scheduler can be iterated in order or
 * objects of a cetain position can be retrieved for iteration.
 *
 * @author Ludovic Orban
 * @param <T>
 */
public class Scheduler<T> implements Iterable<T> {

    public static final Integer DEFAULT_POSITION = 0;
    public static final Integer ALWAYS_FIRST_POSITION = Integer.MIN_VALUE;
    public static final Integer ALWAYS_LAST_POSITION = Integer.MAX_VALUE;

    private final List<Integer> keys = new ArrayList<Integer>();
    private final Map<Integer, List<T>> objects = new TreeMap<Integer, List<T>>();
    private int size = 0;


    public Scheduler() {
    }

    public synchronized void add(T obj, Integer position) {
        List<T> list = objects.get(position);
        if (list == null) {
            if (!keys.contains(position)) {
                keys.add(position);
                Collections.sort(keys);
            }
            list = new ArrayList<T>();
            objects.put(position, list);
        }
        list.add(obj);
        size++;
    }

    public synchronized void remove(T obj) {
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            T o = it.next();
            if (o == obj) {
                it.remove();
                return;
            }
        }
        throw new NoSuchElementException("no such element: " + obj);
    }

    public synchronized SortedSet<Integer> getNaturalOrderPositions() {
        return new TreeSet<Integer>(objects.keySet());
    }

    public synchronized SortedSet<Integer> getReverseOrderPositions() {
        TreeSet<Integer> result = new TreeSet<Integer>(Collections.reverseOrder());
        result.addAll(getNaturalOrderPositions());
        return result;
    }

    public synchronized List<T> getByNaturalOrderForPosition(Integer position) {
        return objects.get(position);
    }

    public synchronized List<T> getByReverseOrderForPosition(Integer position) {
        List<T> result = new ArrayList<T>(getByNaturalOrderForPosition(position));
        Collections.reverse(result);
        return result;
    }

    public synchronized int size() {
        return size;
    }

    @Override
    public Iterator<T> iterator() {
        return new SchedulerNaturalOrderIterator();
    }

    public Iterator<T> reverseIterator() {
        return new SchedulerReverseOrderIterator();
    }

    @Override
    public String toString() {
        return "a Scheduler with " + size() + " object(s) in " + getNaturalOrderPositions().size() + " position(s)";
    }

    /**
     * This iterator supports in-flight updates of the iterated object.
     */
    private final class SchedulerNaturalOrderIterator implements Iterator<T> {
        private int nextKeyIndex;
        private List<T> objectsOfCurrentKey;
        private int objectsOfCurrentKeyIndex;

        private SchedulerNaturalOrderIterator() {
            this.nextKeyIndex = 0;
        }

        @Override
        public void remove() {
            synchronized (Scheduler.this) {
                if (objectsOfCurrentKey == null)
                    throw new NoSuchElementException("iterator not yet placed on an element");

                objectsOfCurrentKeyIndex--;
                objectsOfCurrentKey.remove(objectsOfCurrentKeyIndex);
                if (objectsOfCurrentKey.isEmpty()) {
                    // there are no more objects in the current position's list -> remove it
                    nextKeyIndex--;
                    Integer key = Scheduler.this.keys.get(nextKeyIndex);
                    Scheduler.this.keys.remove(nextKeyIndex);
                    Scheduler.this.objects.remove(key);
                    objectsOfCurrentKey = null;
                }
                Scheduler.this.size--;
            }
        }

        @Override
        public boolean hasNext() {
            synchronized (Scheduler.this) {
                if (objectsOfCurrentKey == null || objectsOfCurrentKeyIndex >= objectsOfCurrentKey.size()) {
                    // we reached the end of the current position's list

                    if (nextKeyIndex < Scheduler.this.keys.size()) {
                        // there is another position after this one
                        Integer currentKey = Scheduler.this.keys.get(nextKeyIndex++);
                        objectsOfCurrentKey = Scheduler.this.objects.get(currentKey);
                        objectsOfCurrentKeyIndex = 0;
                        return true;
                    } else {
                        // there is no other position after this one
                        return false;
                    }
                }

                // there are still objects in the current position's list
                return true;
            }
        }

        @Override
        public T next() {
            synchronized (Scheduler.this) {
                if (!hasNext())
                    throw new NoSuchElementException("iterator bounds reached");
                return objectsOfCurrentKey.get(objectsOfCurrentKeyIndex++);
            }
        }
    }

    /**
     * This iterator supports in-flight updates of the iterated object.
     */
    private final class SchedulerReverseOrderIterator implements Iterator<T> {
        private int nextKeyIndex;
        private List<T> objectsOfCurrentKey;
        private int objectsOfCurrentKeyIndex;

        private SchedulerReverseOrderIterator() {
            synchronized (Scheduler.this) {
                this.nextKeyIndex = Scheduler.this.keys.size() -1;
            }
        }

        @Override
        public void remove() {
            synchronized (Scheduler.this) {
                if (objectsOfCurrentKey == null)
                    throw new NoSuchElementException("iterator not yet placed on an element");

                objectsOfCurrentKeyIndex--;
                objectsOfCurrentKey.remove(objectsOfCurrentKeyIndex);
                if (objectsOfCurrentKey.isEmpty()) {
                    // there are no more objects in the current position's list -> remove it
                    Integer key = Scheduler.this.keys.get(nextKeyIndex+1);
                    Scheduler.this.keys.remove(nextKeyIndex+1);
                    Scheduler.this.objects.remove(key);
                    objectsOfCurrentKey = null;
                }
                Scheduler.this.size--;
            }
        }

        @Override
        public boolean hasNext() {
            synchronized (Scheduler.this) {
                if (objectsOfCurrentKey == null || objectsOfCurrentKeyIndex >= objectsOfCurrentKey.size()) {
                    // we reached the end of the current position's list

                    if (nextKeyIndex >= 0) {
                        // there is another position after this one
                        Integer currentKey = Scheduler.this.keys.get(nextKeyIndex--);
                        objectsOfCurrentKey = Scheduler.this.objects.get(currentKey);
                        objectsOfCurrentKeyIndex = 0;
                        return true;
                    } else {
                        // there is no other position after this one
                        return false;
                    }
                }

                // there are still objects in the current position's list
                return true;
            }
        }

        @Override
        public T next() {
            synchronized (Scheduler.this) {
                if (!hasNext())
                    throw new NoSuchElementException("iterator bounds reached");
                return objectsOfCurrentKey.get(objectsOfCurrentKeyIndex++);
            }
        }
    }

}
