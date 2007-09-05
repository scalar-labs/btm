package bitronix.tm.timer;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.internal.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Timed tasks service.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class TaskScheduler extends Thread implements Service {

    private final static Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    /**
     * Keys of this map are GTRID represented as a String, as per UidGenerator.uidToString.
     * Values are Task objects.
     * TODO: tasks container should not be a map as there is not always a hashable key
     */
    private final Map tasks = Collections.synchronizedMap(new HashMap());
    private boolean active = true;

    public TaskScheduler() {
        // it is up to the ShutdownHandler to control the lifespan of the JVM and give some time for this thread
        // to die gracefully, meaning enough time for all tasks to get executed. This is why it is set as daemon.
        setDaemon(true);
        setName("bitronix-scheduler");
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public synchronized void shutdown() {
        try {
            long gracefulShutdownTime = TransactionManagerServices.getConfiguration().getGracefulShutdownInterval() * 1000;
            if (log.isDebugEnabled()) log.debug("graceful scheduler shutdown interval: " + gracefulShutdownTime + "ms");
            setActive(false);
            join(gracefulShutdownTime);
        } catch (InterruptedException ex) {
            log.error("could not stop the task scheduler within " + TransactionManagerServices.getConfiguration().getGracefulShutdownInterval() + "s");
        }
    }

    /**
     * Schedule a task that will mark the transaction as timed out at the specified date. If this method is called
     * with the same transaction multiple times, the previous timeout date is dropped and replaced by the new one.
     * @param transaction the transaction to mark as timeout.
     * @param executionTime the date at which the transaction must be marked.
     */
    public void scheduleTransactionTimeout(BitronixTransaction transaction, Date executionTime) {
        if (log.isDebugEnabled()) log.debug("scheduling transaction timeout task on " + transaction + " for " + executionTime);
        if (transaction == null)
            throw new IllegalArgumentException("expected a non-null transaction");
        if (executionTime == null)
            throw new IllegalArgumentException("expected a non-null execution date");
        TransactionTimeoutTask task = new TransactionTimeoutTask(transaction, executionTime);
        tasks.put(transaction, task);
        if (log.isDebugEnabled()) log.debug("scheduled " + task + ", total task(s) queued: " + tasks.size());
    }

    public void cancelTransactionTimeout(BitronixTransaction transaction) {
        if (log.isDebugEnabled()) log.debug("cancelling transaction timeout task on " + transaction);
        if (transaction == null)
            throw new IllegalArgumentException("expected a non-null transaction");
        Object task = tasks.remove(transaction);
        if (log.isDebugEnabled()) log.debug("cancelled " + task + ", total task(s) still queued: " + tasks.size());
    }

    public void scheduleRecovery(Date executionTime) {
        if (log.isDebugEnabled()) log.debug("scheduling recovery task for " + executionTime);
        if (executionTime == null)
            throw new IllegalArgumentException("expected a non-null execution date");
        RecoveryTask task = new RecoveryTask(executionTime);
        tasks.put(executionTime, task);
        if (log.isDebugEnabled()) log.debug("scheduled " + task + ", total task(s) queued: " + tasks.size());
    }

    public void schedulePoolShrinking(XAPool xaPool) {
        Date executionTime = xaPool.getNextShrinkDate();
        if (log.isDebugEnabled()) log.debug("scheduling pool shrinking task on " + xaPool + " for " + executionTime);
        if (executionTime == null)
            throw new IllegalArgumentException("expected a non-null execution date");
        PoolShrinkingTask task = new PoolShrinkingTask(xaPool, executionTime);
        tasks.put(executionTime, task);
        if (log.isDebugEnabled()) log.debug("scheduled " + task + ", total task(s) queued: " + tasks.size());
    }

    public void cancelPoolShrinking(XAPool xaPool) {
        if (log.isDebugEnabled()) log.debug("cancelling pool shrinking task on " + xaPool);
        if (xaPool == null)
            throw new IllegalArgumentException("expected a non-null XA pool");
        Iterator it = tasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            if (entry.getValue() instanceof PoolShrinkingTask) {
                PoolShrinkingTask task = (PoolShrinkingTask) entry.getValue();

                if (task.getXaPool() == xaPool) {
                    it.remove();
                    if (log.isDebugEnabled()) log.debug("cancelled " + task + ", total task(s) still queued: " + tasks.size());
                    break;
                }
            } // if
        } // while
    }

    public void run() {
        while (isActive()) {
            try {
                executeElapsedTasks();
                Thread.sleep(500); // execute twice per second. That's enough precision.
            } catch (InterruptedException ex) {
                // ignore
            }
        } // while
    }

    private void executeElapsedTasks() {
        if (tasks.size() == 0)
            return;

        // Copying a map means iterating it so this block must be synchronized as specified by
        // http://java.sun.com/j2se/api/Collections.html#synchronizedMap(java.util.Map)
        Map tasks;
        synchronized (this.tasks) {
            tasks = new HashMap(this.tasks);
        }

        Iterator it = tasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Object key = entry.getKey();
            Task task = (Task) entry.getValue();
            if (task.getExecutionTime().compareTo(new Date()) <= 0) { // if the execution time is now or in the past
                if (log.isDebugEnabled()) log.debug("running " + task);
                try {
                    task.execute();
                    this.tasks.remove(key);
                    if (log.isDebugEnabled()) log.debug("successfully ran " + task);
                } catch (Exception ex) {
                    int taskRetryInterval = 10; // TODO: should that be configurable ?
                    log.warn("error running " + task + ", retrying in " + taskRetryInterval + "s", ex);
                    task.setExecutionTime(new Date(System.currentTimeMillis() + (taskRetryInterval * 1000L)));
                }
            } // if
        } // while

    }

}
