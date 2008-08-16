package bitronix.tm.twopc.executor;

import bitronix.tm.internal.BitronixRuntimeException;

/**
 * This implementation spawns a new thread per request.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class SimpleAsyncExecutor implements Executor {

    public Object submit(Job job) {
        Thread t = new Thread(job);
        t.setDaemon(true);
        t.start();
        return t;
    }

    public void waitFor(Object future, long timeout) {
        Thread t = (Thread) future;
        try {
            t.join(timeout);
        } catch (InterruptedException ex) {
            throw new BitronixRuntimeException("job interrupted", ex);
        }
    }

    public boolean isDone(Object future) {
        Thread t = (Thread) future;
        return !t.isAlive();
    }

    public boolean isUsable() {
        return true;
    }

    public void shutdown() {
    }
}
