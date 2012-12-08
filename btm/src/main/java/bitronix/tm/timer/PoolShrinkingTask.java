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

import bitronix.tm.resource.common.XAPool;

import java.util.Date;

/**
 * This task is used to notify a XA pool to close idle connections.
 *
 * @author Ludovic Orban
 */
public class PoolShrinkingTask extends Task {

    private final XAPool xaPool;

    public PoolShrinkingTask(XAPool xaPool, Date executionTime, TaskScheduler scheduler) {
        super(executionTime, scheduler);
        this.xaPool = xaPool;
    }

    public Object getObject() {
        return xaPool;
    }

    public void execute() throws TaskException {
        try {
            xaPool.shrink();
        } catch (Exception ex) {
            throw new TaskException("error while trying to shrink " + xaPool, ex);
        } finally {
            getTaskScheduler().schedulePoolShrinking(xaPool);
        }
    }

    public String toString() {
        return "a PoolShrinkingTask scheduled for " + getExecutionTime() + " on " + xaPool;
    }

}
