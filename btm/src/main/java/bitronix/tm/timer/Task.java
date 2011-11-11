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

import java.util.Date;

/**
 * Asbtract superclass of all timed tasks.
 *
 * @author lorban
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

    public int compareTo(Task otherTask) {
        return this.executionTime.compareTo(otherTask.executionTime);
    }

    public abstract Object getObject();

    public abstract void execute() throws TaskException;

}
