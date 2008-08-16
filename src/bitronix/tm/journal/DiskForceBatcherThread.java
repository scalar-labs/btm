package bitronix.tm.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Thread that executes disk force batches.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class DiskForceBatcherThread extends Thread {

    private final static Logger log = LoggerFactory.getLogger(DiskForceBatcherThread.class);
    private static DiskForceBatcherThread instance;

    private boolean alive = true;
    private static DiskForceWaitQueue waitQueue = new DiskForceWaitQueue();

    /**
     * Get the single instance of the DiskForceBatcherThread.
     * @return the single instance of the DiskForceBatcherThread.
     */
    public synchronized static DiskForceBatcherThread getInstance() {
        if (instance == null) {
            instance = new DiskForceBatcherThread();
        }
        return instance;
    }

    private DiskForceBatcherThread() {
        setName("bitronix-disk-force-batcher");
        setPriority(Thread.NORM_PRIORITY -1);
        setDaemon(true);
        start();
    }

    /**
     * Thread will run for as long as this flag is not false.
     * @param alive The new flag value.
     */
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * Add the TransactionLogAppender to the wait queue and wait until the disk force is done.
     * @param tla the TransactionLogAppender
     */
    public static void enqueue(TransactionLogAppender tla) {
        waitQueue.enqueue(tla);
        if (log.isDebugEnabled()) log.debug("batching disk force, there are " + waitQueue.size() + " TransactionLogAppender in the wait queue");
        try {
            waitQueue.waitUntilNotContains(tla);
        } catch (InterruptedException ex) {
            if (log.isDebugEnabled()) log.debug("interrupted while waiting for journal log to be forced but disk force will happen anyway");
        }
        if (log.isDebugEnabled()) log.debug("wait queue got emptied, disk force is done");
    }

    private void runForceBatch() throws IOException {
        if (log.isDebugEnabled()) log.debug("waiting for the wait queue to fill up");
        while(alive && waitQueue.isEmpty()) {
            try {
                waitQueue.waitUntilNotEmpty();
            } catch (InterruptedException ex) {
                // ignore
            }
        } // while
        if (!alive) {
            if (log.isDebugEnabled()) log.debug("interrupted while waiting for the queue to fill up");
            return;
        }

        if (log.isDebugEnabled()) log.debug("wait queue is not empty anymore (" + waitQueue.size() + " in queue)");
        DiskForceWaitQueue oldWaitQueue = waitQueue;
        waitQueue = new DiskForceWaitQueue();

        if (log.isDebugEnabled()) log.debug("forcing...");
        oldWaitQueue.head().doForce();
        oldWaitQueue.clear(); // notify threads waiting in this wait queue that the disk force is done
    }

    public void run() {
        if (log.isDebugEnabled()) log.debug("disk force thread is up and running");
        while (alive) {
            try {
                runForceBatch();
            } catch (Exception ex) {
                log.warn("unexpected Exception", ex);
            }
        } // while

        instance = null;
        if (log.isDebugEnabled()) log.debug("disk force thread has terminated");
    }

    public String toString() {
        return "a DiskForceBatcherThread";
    }
}
