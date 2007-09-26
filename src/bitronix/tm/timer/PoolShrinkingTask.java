package bitronix.tm.timer;

import bitronix.tm.resource.common.XAPool;

import java.util.Date;

/**
 * This task is used to run the background recovery.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class PoolShrinkingTask extends Task {

    private XAPool xaPool;

    public PoolShrinkingTask(XAPool xaPool, Date executionTime, TaskScheduler scheduler) {
        super(executionTime, scheduler);
        this.xaPool = xaPool;
    }

    public Object getObject() {
        return xaPool;
    }

    public void execute() throws TaskException {
        try {
            xaPool.shrink();
        } catch (Exception ex) {
            throw new TaskException("error while trying to shrink " + xaPool, ex);
        } finally {
            getTaskScheduler().schedulePoolShrinking(xaPool);
        }
    }

    public String toString() {
        return "a PoolShrinkingTask scheduled for " + getExecutionTime() + " on " + xaPool;
    }

}
