package bitronix.tm.timer;

import java.util.Date;

/**
 * Asbtract superclass of all timed tasks.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public abstract class Task {

    private Date executionTime;
    private TaskScheduler taskScheduler;

    protected Task(Date executionTime, TaskScheduler scheduler) {
        this.executionTime = executionTime;
        this.taskScheduler = scheduler;
    }

    public Date getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Date executionTime) {
        this.executionTime = executionTime;
    }

    protected TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public abstract Object getObject();

    public abstract void execute() throws TaskException;

}
