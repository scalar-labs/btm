package bitronix.tm.internal;

import junit.framework.TestCase;
import bitronix.tm.resource.common.ResourceBean;

import java.util.Iterator;
import java.util.Set;
import java.util.List;

public class ResourceSchedulerTest extends TestCase {
    
    public void testNaturalOrdering() throws Exception {
        ResourceScheduler resourceScheduler = new ResourceScheduler();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs2 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs3 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs4 = new XAResourceHolderState(null, new MockResourceBean(10));

        resourceScheduler.addResource(xarhs0);
        resourceScheduler.addResource(xarhs1);
        resourceScheduler.addResource(xarhs2);
        resourceScheduler.addResource(xarhs3);
        resourceScheduler.addResource(xarhs4);

        assertEquals("a ResourceScheduler with 5 resource(s) in 3 position(s)", resourceScheduler.toString());

        /* testing natural order priorities */
        assertEquals(5, resourceScheduler.size());
        Set priorities = resourceScheduler.getNaturalOrderPositions();
        assertEquals(3, priorities.size());

        Iterator it = priorities.iterator();
        Object key0 = it.next();
        Object key1 = it.next();
        Object key2 = it.next();
        assertFalse(it.hasNext());

        List list0 = resourceScheduler.getNaturalOrderResourcesForPosition(key0);
        assertEquals(1, list0.size());
        assertTrue(xarhs3 == list0.get(0));

        List list1 = resourceScheduler.getNaturalOrderResourcesForPosition(key1);
        assertEquals(3, list1.size());
        assertTrue(xarhs0 == list1.get(0));
        assertTrue(xarhs1 == list1.get(1));
        assertTrue(xarhs2 == list1.get(2));

        List list2 = resourceScheduler.getNaturalOrderResourcesForPosition(key2);
        assertEquals(1, list2.size());
        assertTrue(xarhs4 == list2.get(0));
    }

    public void testReverseOrdering() throws Exception {
        ResourceScheduler resourceScheduler = new ResourceScheduler();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs2 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs3 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs4 = new XAResourceHolderState(null, new MockResourceBean(10));

        resourceScheduler.addResource(xarhs0);
        resourceScheduler.addResource(xarhs1);
        resourceScheduler.addResource(xarhs2);
        resourceScheduler.addResource(xarhs3);
        resourceScheduler.addResource(xarhs4);

        assertEquals("a ResourceScheduler with 5 resource(s) in 3 position(s)", resourceScheduler.toString());

        Set reverseOrderPriorities = resourceScheduler.getReverseOrderPositions();
        assertEquals(3, reverseOrderPriorities.size());

        Iterator itReverse = reverseOrderPriorities.iterator();
        Object key0r = itReverse.next();
        Object key1r = itReverse.next();
        Object key2r = itReverse.next();
        assertFalse(itReverse.hasNext());

        List list0r = resourceScheduler.getReverseOrderResourcesForPosition(key0r);
        assertEquals(1, list0r.size());
        assertTrue(xarhs4 == list0r.get(0));

        List list1r = resourceScheduler.getReverseOrderResourcesForPosition(key1r);
        assertEquals(3, list1r.size());
        assertTrue(xarhs2 == list1r.get(0));
        assertTrue(xarhs1 == list1r.get(1));
        assertTrue(xarhs0 == list1r.get(2));

        List list2r = resourceScheduler.getReverseOrderResourcesForPosition(key2r);
        assertEquals(1, list2r.size());
        assertTrue(xarhs3 == list2r.get(0));
    }

    public void testIterator() {
        ResourceScheduler resourceScheduler = new ResourceScheduler();

        XAResourceHolderState xarhs0 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs1 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs2 = new XAResourceHolderState(null, new MockResourceBean(1));
        XAResourceHolderState xarhs3 = new XAResourceHolderState(null, new MockResourceBean(0));
        XAResourceHolderState xarhs4 = new XAResourceHolderState(null, new MockResourceBean(10));

        resourceScheduler.addResource(xarhs0);
        resourceScheduler.addResource(xarhs1);
        resourceScheduler.addResource(xarhs2);
        resourceScheduler.addResource(xarhs3);
        resourceScheduler.addResource(xarhs4);

        assertEquals("a ResourceScheduler with 5 resource(s) in 3 position(s)", resourceScheduler.toString());

        Iterator it = resourceScheduler.iterator();
        assertTrue(it.hasNext());
        assertTrue(xarhs3 == it.next());
        assertTrue(xarhs0 == it.next());
        assertTrue(xarhs1 == it.next());
        assertTrue(xarhs2 == it.next());
        assertTrue(xarhs4 == it.next());
        assertFalse(it.hasNext());
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
