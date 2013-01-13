package bitronix.tm.utils;

import junit.framework.TestCase;

/**
 * @author Ludovic Orban
 */
public class MonotonicClockTest extends TestCase {

    public void testPrecision() throws Exception {
        for (int i = 0; i < 100; i++) {
            long monoTime = MonotonicClock.currentTimeMillis();
            long wallTime = System.currentTimeMillis();
            assertTrue(Math.abs(wallTime - monoTime) < 5L);
            Thread.sleep(10);
        }
    }

}
