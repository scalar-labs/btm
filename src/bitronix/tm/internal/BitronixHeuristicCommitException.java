package bitronix.tm.internal;

import javax.transaction.HeuristicCommitException;

/**
 * Subclass of {@link javax.transaction.HeuristicCommitException} supporting nested {@link Throwable}s.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BitronixHeuristicCommitException extends HeuristicCommitException {

    public BitronixHeuristicCommitException(String string) {
        super(string);
    }

    public BitronixHeuristicCommitException(String string, Throwable t) {
        super(string);
        initCause(t);
    }

}
