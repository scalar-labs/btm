package bitronix.tm.timer;

/**
 * Thrown when an error occurs during the execution of a task.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TaskException extends Exception {
    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
