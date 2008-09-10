package bitronix.tm.timer;

import bitronix.tm.recovery.Recoverer;
import junit.framework.TestCase;

import java.util.Date;


public class TaskSchedulerTest extends TestCase {
    
    public void testRecoveryTask() throws Exception {
        TaskScheduler ts = new TaskScheduler();
        ts.start();

        Recoverer recoverer = new Recoverer();
        ts.scheduleRecovery(recoverer, new Date());
        assertEquals(1, ts.countTasksQueued());
        Thread.sleep(1100);
        assertEquals(1, ts.countTasksQueued());

        ts.cancelRecovery(recoverer);
        assertEquals(0, ts.countTasksQueued());
        Thread.sleep(1100);
        assertEquals(0, ts.countTasksQueued());

        ts.setActive(false);
        ts.join();
    }

}
