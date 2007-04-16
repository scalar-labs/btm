package bitronix.tm.twopc.executor;

/**
 * This implementation executes submitted jobs synchronously.
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class SyncExecutor implements Executor {

    public Object submit(Runnable job) {
        job.run();
        return new Object();
    }

    public void waitFor(Object future, long timeout) {
    }

    public boolean isDone(Object future) {
        return true;
    }

    public boolean isUsable() {
        return true;
    }

    public void shutdown() {
    }
}
