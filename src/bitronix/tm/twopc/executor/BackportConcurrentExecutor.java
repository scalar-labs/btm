package bitronix.tm.twopc.executor;

/**
 * Abstraction of the <code>java.util.concurrent</code>
 * <a href="http://www.dcl.mathcs.emory.edu/util/backport-util-concurrent/">backport</a> implementation.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BackportConcurrentExecutor extends ConcurrentExecutor {

    private final static String[] implementations = {
        "edu.emory.mathcs.backport.java.util.concurrent.Executors",
        "edu.emory.mathcs.backport.java.util.concurrent.ExecutorService",
        "edu.emory.mathcs.backport.java.util.concurrent.Future",
        "edu.emory.mathcs.backport.java.util.concurrent.TimeUnit"
    };

    public BackportConcurrentExecutor() {
        super(implementations);
    }
}
