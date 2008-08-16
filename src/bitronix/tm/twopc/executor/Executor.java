package bitronix.tm.twopc.executor;

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.utils.Service;

/**
 * Thread pool interface required by the two-phase commit logic.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
     * Check if the thread pool can be used. The thread pool migh rely on an underlying implementation that may not be
     * available. All other methods will throw a {@link BitronixRuntimeException} when called if this method returns false.
     * @return true if the {@link Executor} can be used, false otherwise.
     */
    public boolean isUsable();

    /**
     * Shutdown the thead pool.
     */
    public void shutdown();
    
}
