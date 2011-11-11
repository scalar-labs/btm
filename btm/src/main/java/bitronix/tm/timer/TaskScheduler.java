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
package bitronix.tm.timer;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.MonotonicClock;
import bitronix.tm.utils.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Timed tasks service.
 *
 * @author lorban
 */
public class TaskScheduler extends Thread implements Service {

    private final static Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    private final SortedSet<Task> tasks;
    private final Lock tasksLock;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public TaskScheduler() {
        // it is up to the ShutdownHandler to control the lifespan of the JVM and give some time for this thread
        // to die gracefully, meaning enough time for all tasks to get executed. This is why it is set as daemon.
        setDaemon(true);
        setName("bitronix-task-scheduler");

        SortedSet<Task> tasks;
        Lock tasksLock;
        try {
            @SuppressWarnings("unchecked")
            Class<SortedSet<Task>> clazz = ClassLoaderUtils.loadClass("java.util.concurrent.ConcurrentSkipListSet");
            tasks = clazz.newInstance();
            tasksLock = null;
            if (log.isDebugEnabled()) log.debug("task scheduler backed by ConcurrentSkipListSet");
        } catch (Exception e) {
            tasks = new TreeSet<Task>();
            tasksLock = new ReentrantLock();
            if (log.isDebugEnabled()) log.debug("task scheduler backed by locked TreeSet");
        }
        this.tasks = tasks;
        this.tasksLock = tasksLock;
    }

    private void lock() {
        if (tasksLock != null) {
            tasksLock.lock();
        }
    }

    private void unlock() {
        if (tasksLock != null) {
            tasksLock.unlock();
        }
    }

    /**
     * Get the amount of tasks currently queued.
     * @return the amount of tasks currently queued.
     */
    public int countTasksQueued() {
        lock();
        try {
            return tasks.size();
        } finally {
            unlock();
        }
    }

    public void shutdown() {
        boolean wasActive = setActive(false);

        if (wasActive) {
            try {
                long gracefulShutdownTime = TransactionManagerServices.getConfiguration().getGracefulShutdownInterval() * 1000;
                if (log.isDebugEnabled()) log.debug("graceful scheduler shutdown interval: " + gracefulShutdownTime + "ms");
                join(gracefulShutdownTime);
            } catch (InterruptedException ex) {
                log.error("could not stop the task scheduler within " + TransactionManagerServices.getConfiguration().getGracefulShutdownInterval() + "s");
            }
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

    void addTask(Task task) {
        lock();
        try {
            removeTaskByObject(task.getObject());
            tasks.add(task);
        } finally {
            unlock();
        }
    }

    boolean removeTaskByObject(Object obj) {
        lock();
        try {
            if (log.isDebugEnabled()) log.debug("removing task by " + obj);

            for (Task task : tasks) {
                if (task.getObject() == obj) {
                    tasks.remove(task);
                    if (log.isDebugEnabled()) log.debug("cancelled " + task + ", total task(s) still queued: " + tasks.size());
                    return true;
                }
            }
            return false;
        } finally {
            unlock();
        }
    }

    boolean setActive(boolean active) {
        return this.active.getAndSet(active);
    }

    private boolean isActive() {
        return active.get();
    }

    public void run() {
        while (isActive()) {
            try {
                executeElapsedTasks();
                Thread.sleep(500); // execute twice per second. That's enough precision.
            } catch (InterruptedException ex) {
                // ignore
            }
        }
    }

    private void executeElapsedTasks() {
        lock();
        try {
            if (this.tasks.isEmpty())
                return;

            Set<Task> toRemove = new HashSet<Task>();
            for (Task task : tasks) {
                if (task.getExecutionTime().compareTo(new Date(MonotonicClock.currentTimeMillis())) <= 0) {
                    // if the execution time is now or in the past
                    if (log.isDebugEnabled()) log.debug("running " + task);
                    try {
                        task.execute();
                        if (log.isDebugEnabled()) log.debug("successfully ran " + task);
                    } catch (Exception ex) {
                        log.warn("error running " + task, ex);
                    } finally {
                        toRemove.add(task);
                        if (log.isDebugEnabled()) log.debug("total task(s) still queued: " + tasks.size());
                    }
                } // if
            }
            this.tasks.removeAll(toRemove);
        } finally {
            unlock();
        }
    }

}
