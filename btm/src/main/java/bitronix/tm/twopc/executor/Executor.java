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
package bitronix.tm.twopc.executor;

import bitronix.tm.utils.Service;

/**
 * Thread pool interface required by the two-phase commit logic.
 *
 * @author Ludovic Orban
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
