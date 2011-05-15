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
package bitronix.tm.twopc.executor;

import bitronix.tm.utils.Service;

/**
 * Thread pool interface required by the two-phase commit logic.
 *
 * @author lorban
 */
public interface Executor extends Service {

    /**
     * Submit a job to be executed by the thread pool.
     * @param job the {@link Runnable} to execute.
     * @return an object used to monitor the execution of the submitted {@link Runnable}.
     */
    public Object submit(Job job);

    /**
     * Wait for the job represented by the future to terminate. The call to this method will block until the job
     * finished its execution or the specified timeout elapsed.
     * @param future the future representing the job as returned by {@link #submit}.
     * @param timeout if the job did not finish during the specified timeout in milliseconds, this method returns anyway.
     */
    public void waitFor(Object future, long timeout);

    /**
     * Check if the thread pool has terminated the execution of the job represented by a future.
     * @param future the future representing the job as returned by {@link #submit}.
     * @return true if the job is done, false otherwise.
     */
    public boolean isDone(Object future);

    /**
     * Shutdown the thead pool.
     */
    public void shutdown();
    
}
