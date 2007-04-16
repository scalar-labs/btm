package bitronix.tm.timer;

import java.util.Date;

/**
 * Asbtract superclass of all timed tasks.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public abstract class Task {

    protected Date executionTime;

    protected Task(Date executionTime) {
        this.executionTime = executionTime;
    }

    public Date getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Date executionTime) {
        this.executionTime = executionTime;
    }

    public abstract void execute() throws TaskException;

}
