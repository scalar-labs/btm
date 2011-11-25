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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.recovery.Recoverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This task is used to run the background recovery.
 *
 * @author lorban
 */
public class RecoveryTask extends Task {

    private final static Logger log = LoggerFactory.getLogger(RecoveryTask.class);

    private final Recoverer recoverer;

    public RecoveryTask(Recoverer recoverer, Date executionTime, TaskScheduler scheduler) {
        super(executionTime, scheduler);
        this.recoverer = recoverer;
    }

    public Object getObject() {
        return recoverer;
    }

    public void execute() throws TaskException {
        if (log.isDebugEnabled()) log.debug("running recovery");
        Thread recovery = new Thread(recoverer);
        recovery.setName("bitronix-recovery-thread");
        recovery.setDaemon(true);
        recovery.setPriority(Thread.NORM_PRIORITY -1);
        recovery.start();

        Date nextExecutionDate = new Date(getExecutionTime().getTime() + (TransactionManagerServices.getConfiguration().getBackgroundRecoveryIntervalSeconds() * 1000L));
        if (log.isDebugEnabled()) log.debug("rescheduling recovery for " + nextExecutionDate);
        getTaskScheduler().scheduleRecovery(recoverer, nextExecutionDate);
    }

    public String toString() {
        return "a RecoveryTask scheduled for " + getExecutionTime();
    }
    
}
