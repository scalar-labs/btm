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
import junit.framework.TestCase;

import java.util.Date;


/**
 *
 * @author lorban
 */
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

        ts.shutdown();
    }

}
