package bitronix.tm.recovery;

/**
 * {@link Recoverer} Management interface.
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public interface RecovererMBean {

    public void run();

    public int getCommittedCount();

    public int getRolledbackCount();

    public Exception getCompletionException();

}
