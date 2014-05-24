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

import java.util.Date;

/**
 * Asbtract superclass of all timed tasks.
 *
 * @author Ludovic Orban
 */
public abstract class Task implements Comparable<Task> {

    private final Date executionTime;
    private final TaskScheduler taskScheduler;

    protected Task(Date executionTime, TaskScheduler scheduler) {
        this.executionTime = executionTime;
        this.taskScheduler = scheduler;
    }

    public Date getExecutionTime() {
        return executionTime;
    }

    protected TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    @Override
    public int compareTo(Task otherTask) {
        return this.executionTime.compareTo(otherTask.executionTime);
    }

    public abstract Object getObject();

    public abstract void execute() throws TaskException;

}
