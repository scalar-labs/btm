package bitronix.tm.recovery;

/**
 * {@link Recoverer} Management interface.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public interface RecovererMBean {

    public void run();

    public int getCommittedCount();

    public int getRolledbackCount();

    public Exception getCompletionException();

}
