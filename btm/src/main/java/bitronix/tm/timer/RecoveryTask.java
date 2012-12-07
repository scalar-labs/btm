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
        if (log.isDebugEnabled()) { log.debug("running recovery"); }
        Thread recovery = new Thread(recoverer);
        recovery.setName("bitronix-recovery-thread");
        recovery.setDaemon(true);
        recovery.setPriority(Thread.NORM_PRIORITY -1);
        recovery.start();

        Date nextExecutionDate = new Date(getExecutionTime().getTime() + (TransactionManagerServices.getConfiguration().getBackgroundRecoveryIntervalSeconds() * 1000L));
        if (log.isDebugEnabled()) { log.debug("rescheduling recovery for " + nextExecutionDate); }
        getTaskScheduler().scheduleRecovery(recoverer, nextExecutionDate);
    }

    public String toString() {
        return "a RecoveryTask scheduled for " + getExecutionTime();
    }
    
}
