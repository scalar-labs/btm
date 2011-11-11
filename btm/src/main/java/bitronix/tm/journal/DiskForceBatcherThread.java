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
package bitronix.tm.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread that executes disk force batches.
 *
 * @author lorban
 */
public final class DiskForceBatcherThread extends Thread {

    private final static Logger log = LoggerFactory.getLogger(DiskForceBatcherThread.class);
    private static volatile DiskForceBatcherThread instance;

    private final AtomicBoolean alive = new AtomicBoolean();
    private volatile DiskForceWaitQueue waitQueue = new DiskForceWaitQueue();

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
        setPriority(Thread.NORM_PRIORITY - 1);
        setDaemon(true);
        alive.set(true);
        start();
    }

    /**
     * Thread will run for as long as this flag is not false.
     * @param alive The new flag value.
     * @return the old flag value.
     */
    public boolean setAlive(boolean alive) {
        return this.alive.getAndSet(alive);
    }

    /**
     * Add the TransactionLogAppender to the wait queue and wait until the disk force is done.
     * @param tla the TransactionLogAppender
     */
    public void enqueue(TransactionLogAppender tla) {
        DiskForceWaitQueue currrentWaitQueue = waitQueue;
        while (!currrentWaitQueue.enqueue(tla)) {
            if (log.isDebugEnabled()) log.debug("current DiskForceWaitQueue [" + currrentWaitQueue + "] is cleared, trying next one: [" + waitQueue + "]");
            currrentWaitQueue = waitQueue;
        }
        if (log.isDebugEnabled()) log.debug("batching disk force, there are " + currrentWaitQueue.size() + " TransactionLogAppender(s) in the wait queue");
        try {
            currrentWaitQueue.waitUntilNotContains(tla);
        } catch (InterruptedException ex) {
            if (log.isDebugEnabled()) log.debug("interrupted while waiting for journal log to be forced, ignored as disk force will happen anyway");
        }
        if (log.isDebugEnabled()) log.debug("wait queue got emptied, disk force is done");
    }

    private void runForceBatch() throws IOException {
        if (log.isDebugEnabled()) log.debug("waiting for the wait queue to fill up");
        while(alive.get() && waitQueue.isEmpty()) {
            try {
                waitQueue.waitUntilNotEmpty();
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        if (!alive.get()) {
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
        while (alive.get()) {
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
