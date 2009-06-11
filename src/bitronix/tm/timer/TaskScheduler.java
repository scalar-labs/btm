package bitronix.tm.timer;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.utils.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Timed tasks service.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TaskScheduler extends Thread implements Service {

    private final static Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    private final List tasks = Collections.synchronizedList(new ArrayList());
    private boolean active = true;

    public TaskScheduler() {
        // it is up to the ShutdownHandler to control the lifespan of the JVM and give some time for this thread
        // to die gracefully, meaning enough time for all tasks to get executed. This is why it is set as daemon.
        setDaemon(true);
        setName("bitronix-scheduler");
    }

    /**
     * Get the amount of tasks currently queued.
     * @return the amount of tasks currently queued.
     */
    public int countTasksQueued() {
        return tasks.size();
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

        TransactionTimeoutTask task = new TransactionTimeoutTask(transaction, executionTime, this);
        addTask(task);
        if (log.isDebugEnabled()) log.debug("scheduled " + task + ", total task(s) queued: " + tasks.size());
    }

    /**
     * Cancel the task that will mark the transaction as timed out at the specified date.
     * @param transaction the transaction to mark as timeout.
     */
    public void cancelTransactionTimeout(BitronixTransaction transaction) {
        if (log.isDebugEnabled()) log.debug("cancelling transaction timeout task on " + transaction);
        if (transaction == null)
            throw new IllegalArgumentException("expected a non-null transaction");

        if (!removeTaskByObject(transaction))
            if (log.isDebugEnabled()) log.debug("no task found based on object " + transaction);
    }

    /**
     * Schedule a task that will run background recovery at the specified date.
     * @param recoverer the recovery implementation to use.
     * @param executionTime the date at which the transaction must be marked.
     */
    public void scheduleRecovery(Recoverer recoverer, Date executionTime) {
        if (log.isDebugEnabled()) log.debug("scheduling recovery task for " + executionTime);
        if (recoverer == null)
            throw new IllegalArgumentException("expected a non-null recoverer");
        if (executionTime == null)
            throw new IllegalArgumentException("expected a non-null execution date");

        RecoveryTask task = new RecoveryTask(recoverer, executionTime, this);
        addTask(task);
        if (log.isDebugEnabled()) log.debug("scheduled " + task + ", total task(s) queued: " + tasks.size());
    }

    /**
     * Cancel the task that will run background recovery at the specified date.
     * @param recoverer the recovery implementation to use.
     */
    public void cancelRecovery(Recoverer recoverer) {
        if (log.isDebugEnabled()) log.debug("cancelling recovery task");

        if (!removeTaskByObject(recoverer))
            if (log.isDebugEnabled()) log.debug("no task found based on object " + recoverer);
    }

    /**
     * Schedule a task that will tell a XA pool to close idle connections. The execution time will be provided by the
     * XA pool itself via the {@link bitronix.tm.resource.common.XAPool#getNextShrinkDate()}.
     * @param xaPool the XA pool to notify.
     */
    public void schedulePoolShrinking(XAPool xaPool) {
        Date executionTime = xaPool.getNextShrinkDate();
        if (log.isDebugEnabled()) log.debug("scheduling pool shrinking task on " + xaPool + " for " + executionTime);
        if (executionTime == null)
            throw new IllegalArgumentException("expected a non-null execution date");

        PoolShrinkingTask task = new PoolShrinkingTask(xaPool, executionTime, this);
        addTask(task);
        if (log.isDebugEnabled()) log.debug("scheduled " + task + ", total task(s) queued: " + tasks.size());
    }

    /**
     * Cancel the task that will tell a XA pool to close idle connections.
     * @param xaPool the XA pool to notify.
     */
    public void cancelPoolShrinking(XAPool xaPool) {
        if (log.isDebugEnabled()) log.debug("cancelling pool shrinking task on " + xaPool);
        if (xaPool == null)
            throw new IllegalArgumentException("expected a non-null XA pool");

        if (!removeTaskByObject(xaPool))
            if (log.isDebugEnabled()) log.debug("no task found based on object " + xaPool);
    }

    private void addTask(Task task) {
        synchronized (tasks) {
            removeTaskByObject(task.getObject());
            tasks.add(task);
        }
    }

    private boolean removeTaskByObject(Object obj) {
        synchronized (tasks) {
            if (log.isDebugEnabled()) log.debug("removing task by " + obj);
            for (int i = 0; i < tasks.size(); i++) {
                Task task = (Task) tasks.get(i);

                if (task.getObject() == obj) {
                    tasks.remove(task);
                    if (log.isDebugEnabled()) log.debug("cancelled " + task + ", total task(s) still queued: " + tasks.size());
                    return true;
                }
            }
            return false;
        }
    }

    void setActive(boolean active) {
        this.active = active;
    }

    private boolean isActive() {
        return active;
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
        if (this.tasks.size() == 0)
            return;

        // Copying a collection means iterating it so this block must be synchronized
        List tasks;
        synchronized (this.tasks) {
            tasks = new ArrayList(this.tasks);
        }

        Iterator it = tasks.iterator();
        while (it.hasNext()) {
            Task task = (Task) it.next();
            if (task.getExecutionTime().compareTo(new Date()) <= 0) { // if the execution time is now or in the past
                if (log.isDebugEnabled()) log.debug("running " + task);
                try {
                    task.execute();
                    if (log.isDebugEnabled()) log.debug("successfully ran " + task);
                } catch (Exception ex) {
                    log.warn("error running " + task, ex);
                } finally {
                    this.tasks.remove(task);
                    if (log.isDebugEnabled()) log.debug("total task(s) still queued: " + tasks.size());
                }
            } // if
        } // while

    }

}
