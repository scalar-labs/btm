package bitronix.tm.twopc.executor;

/**
 * This implementation executes submitted jobs synchronously.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class SyncExecutor implements Executor {

    public Object submit(Job job) {
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
