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

import bitronix.tm.resource.common.XAPool;

import java.util.Date;

/**
 * This task is used to notify a XA pool to close idle connections.
 *
 * @author lorban
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
