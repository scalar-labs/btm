/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.utils;

import junit.framework.TestCase;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.internal.XAResourceHolderState;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.List;

/**
 *
 * @author lorban
 */
public class SchedulerTest extends TestCase {

    public void testNaturalOrdering() throws Exception {
        Scheduler<XAResourceHolderState> resourceScheduler = new Scheduler<XAResourceHolderState>();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs2 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs3 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs4 = new XAResourceHolderState(null, new MockResourceBean(10));

        resourceScheduler.add(xarhs0, xarhs0.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs1, xarhs1.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs2, xarhs2.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs3, xarhs3.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs4, xarhs4.getTwoPcOrderingPosition());

        assertEquals("a Scheduler with 5 object(s) in 3 position(s)", resourceScheduler.toString());

        /* testing natural order priorities */
        assertEquals(5, resourceScheduler.size());
        Set<Integer> priorities = resourceScheduler.getNaturalOrderPositions();
        assertEquals(3, priorities.size());

        Iterator<Integer> it = priorities.iterator();
        Integer key0 = it.next();
        Integer key1 = it.next();
        Integer key2 = it.next();
        assertFalse(it.hasNext());

        List<XAResourceHolderState> list0 = resourceScheduler.getByNaturalOrderForPosition(key0);
        assertEquals(1, list0.size());
        assertTrue(xarhs3 == list0.get(0));

        List<XAResourceHolderState> list1 = resourceScheduler.getByNaturalOrderForPosition(key1);
        assertEquals(3, list1.size());
        assertTrue(xarhs0 == list1.get(0));
        assertTrue(xarhs1 == list1.get(1));
        assertTrue(xarhs2 == list1.get(2));

        List<XAResourceHolderState> list2 = resourceScheduler.getByNaturalOrderForPosition(key2);
        assertEquals(1, list2.size());
        assertTrue(xarhs4 == list2.get(0));
    }

    public void testReverseOrdering() throws Exception {
        Scheduler<XAResourceHolderState> resourceScheduler = new Scheduler<XAResourceHolderState>();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs2 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs3 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs4 = new XAResourceHolderState(null, new MockResourceBean(10));

        resourceScheduler.add(xarhs0, xarhs0.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs1, xarhs1.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs2, xarhs2.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs3, xarhs3.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs4, xarhs4.getTwoPcOrderingPosition());

        assertEquals("a Scheduler with 5 object(s) in 3 position(s)", resourceScheduler.toString());

        Set<Integer> reverseOrderPriorities = resourceScheduler.getReverseOrderPositions();
        assertEquals(3, reverseOrderPriorities.size());

        Iterator<Integer> itReverse = reverseOrderPriorities.iterator();
        Integer key0r = itReverse.next();
        Integer key1r = itReverse.next();
        Integer key2r = itReverse.next();
        assertFalse(itReverse.hasNext());

        List<XAResourceHolderState> list0r = resourceScheduler.getByReverseOrderForPosition(key0r);
        assertEquals(1, list0r.size());
        assertTrue(xarhs4 == list0r.get(0));

        List<XAResourceHolderState> list1r = resourceScheduler.getByReverseOrderForPosition(key1r);
        assertEquals(3, list1r.size());
        assertTrue(xarhs2 == list1r.get(0));
        assertTrue(xarhs1 == list1r.get(1));
        assertTrue(xarhs0 == list1r.get(2));

        List<XAResourceHolderState> list2r = resourceScheduler.getByReverseOrderForPosition(key2r);
        assertEquals(1, list2r.size());
        assertTrue(xarhs3 == list2r.get(0));
    }

    public void testIterator() {
        Scheduler<XAResourceHolderState> resourceScheduler = new Scheduler<XAResourceHolderState>();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs2 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs3 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs4 = new XAResourceHolderState(null, new MockResourceBean(10));

        resourceScheduler.add(xarhs0, xarhs0.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs1, xarhs1.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs2, xarhs2.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs3, xarhs3.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs4, xarhs4.getTwoPcOrderingPosition());

        assertEquals("a Scheduler with 5 object(s) in 3 position(s)", resourceScheduler.toString());

        Iterator<XAResourceHolderState> it = resourceScheduler.iterator();
        assertTrue(it.hasNext());
        assertTrue(xarhs3 == it.next());
        assertTrue(xarhs0 == it.next());
        assertTrue(xarhs1 == it.next());
        assertTrue(xarhs2 == it.next());
        assertTrue(xarhs4 == it.next());
        assertFalse(it.hasNext());

        it = resourceScheduler.iterator();
        assertTrue(it.hasNext());
        assertTrue(xarhs3 == it.next());
        it.remove();
        assertTrue(xarhs0 == it.next());
        it.remove();
        assertTrue(xarhs1 == it.next());
        it.remove();
        assertTrue(xarhs2 == it.next());
        it.remove();
        assertTrue(xarhs4 == it.next());
        it.remove();
        assertFalse(it.hasNext());
        assertEquals(0, resourceScheduler.size());
    }

    public void testReverseIterator() {
        Scheduler<XAResourceHolderState> resourceScheduler = new Scheduler<XAResourceHolderState>();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs2 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs3 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs4 = new XAResourceHolderState(null, new MockResourceBean(10));

        resourceScheduler.add(xarhs0, xarhs0.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs1, xarhs1.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs2, xarhs2.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs3, xarhs3.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs4, xarhs4.getTwoPcOrderingPosition());

        assertEquals("a Scheduler with 5 object(s) in 3 position(s)", resourceScheduler.toString());

        Iterator it = resourceScheduler.reverseIterator();
        assertTrue(it.hasNext());

        assertTrue(xarhs4 == it.next());
        assertTrue(xarhs0 == it.next());
        assertTrue(xarhs1 == it.next());
        assertTrue(xarhs2 == it.next());
        assertTrue(xarhs3 == it.next());

        assertFalse(it.hasNext());
    }

    public void testRemove() {
        Scheduler<XAResourceHolderState> resourceScheduler = new Scheduler<XAResourceHolderState>();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(1));

        resourceScheduler.add(xarhs0, xarhs0.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs1, xarhs1.getTwoPcOrderingPosition());

        resourceScheduler.remove(xarhs0);
        resourceScheduler.add(xarhs0, xarhs0.getTwoPcOrderingPosition());

        Iterator<XAResourceHolderState> it = resourceScheduler.iterator();
        assertTrue(it.hasNext());
        assertTrue(xarhs0 == it.next());
        it.remove();
        assertTrue(xarhs1 == it.next());
        it.remove();
    }

    public void testReverseRemove() {
        Scheduler<XAResourceHolderState> resourceScheduler = new Scheduler<XAResourceHolderState>();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(1));

        resourceScheduler.add(xarhs0, xarhs0.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs1, xarhs1.getTwoPcOrderingPosition());

        resourceScheduler.remove(xarhs0);
        resourceScheduler.add(xarhs0, xarhs0.getTwoPcOrderingPosition());

        Iterator<XAResourceHolderState> it = resourceScheduler.reverseIterator();
        assertTrue(it.hasNext());
        assertTrue(xarhs1 == it.next());
        it.remove();
        assertTrue(xarhs0 == it.next());
        it.remove();
    }

    public void testHasNext() {
        Scheduler<XAResourceHolderState> resourceScheduler = new Scheduler<XAResourceHolderState>();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(10));

        resourceScheduler.add(xarhs0, xarhs0.getTwoPcOrderingPosition());
        resourceScheduler.add(xarhs1, xarhs1.getTwoPcOrderingPosition());


        Iterator<XAResourceHolderState> it = resourceScheduler.iterator();

        for (int i=0; i<10 ;i++) {
            assertTrue(it.hasNext());
        }
        it.next();
        for (int i=0; i<10 ;i++) {
            assertTrue(it.hasNext());
        }
        it.next();
        for (int i=0; i<10 ;i++) {
            assertFalse(it.hasNext());
        }

        try {
            it.next();
            fail("expected NoSuchElementException");
        } catch (NoSuchElementException ex) {
            // expected
        }
    }

    private static int counter = 0;
    private static int incCounter() {
        return counter++;
    }

    private class MockResourceBean extends ResourceBean {

        private int number;
        private int commitOrderingPosition;

        private MockResourceBean(int commitOrderingPosition) {
            this.number = incCounter();
            this.commitOrderingPosition = commitOrderingPosition;
        }


        public int getTwoPcOrderingPosition() {
            return commitOrderingPosition;
        }

        public String toString() {
            return "a MockResourceBean #" + number;
        }
    }

}
