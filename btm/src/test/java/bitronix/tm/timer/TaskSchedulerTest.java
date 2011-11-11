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
package bitronix.tm.timer;

import bitronix.tm.recovery.Recoverer;
import bitronix.tm.utils.MonotonicClock;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 *
 * @author lorban
 */
public class TaskSchedulerTest extends TestCase {

    private TaskScheduler ts;

    @Override
    protected void setUp() throws Exception {
        ts = new TaskScheduler();
        ts.start();
    }

    @Override
    protected void tearDown() throws Exception {
        assertEquals(0, ts.countTasksQueued());
        ts.shutdown();
    }


    public void testRecoveryTask() throws Exception {
        Recoverer recoverer = new Recoverer();
        ts.scheduleRecovery(recoverer, new Date());
        assertEquals(1, ts.countTasksQueued());
        Thread.sleep(1100);
        assertEquals(1, ts.countTasksQueued());

        ts.cancelRecovery(recoverer);
        assertEquals(0, ts.countTasksQueued());
        Thread.sleep(1100);
        assertEquals(0, ts.countTasksQueued());
    }

    public void testTaskOrdering() throws Exception {
        List<SimpleTask> result = Collections.synchronizedList(new ArrayList<SimpleTask>());

        ts.addTask(new SimpleTask(new Date(MonotonicClock.currentTimeMillis() + 100), ts, 0, result));
        ts.addTask(new SimpleTask(new Date(MonotonicClock.currentTimeMillis() + 200), ts, 1, result));
        ts.addTask(new SimpleTask(new Date(MonotonicClock.currentTimeMillis() + 300), ts, 2, result));

        ts.join(1000);

        assertEquals(0, result.get(0).getObject());
        assertEquals(1, result.get(1).getObject());
        assertEquals(2, result.get(2).getObject());
    }

    private static class SimpleTask extends Task {

        private final Object obj;
        private final List<SimpleTask> result;

        protected SimpleTask(Date executionTime, TaskScheduler scheduler, Object obj, List<SimpleTask> result) {
            super(executionTime, scheduler);
            this.obj = obj;
            this.result = result;
        }

        @Override
        public Object getObject() {
            return obj;
        }

        @Override
        public void execute() throws TaskException {
            result.add(this);
        }
    }

}
