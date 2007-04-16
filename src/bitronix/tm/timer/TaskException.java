package bitronix.tm.timer;

/**
 * Thrown when an error occurs during the execution of a task.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class TaskException extends Exception {
    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
