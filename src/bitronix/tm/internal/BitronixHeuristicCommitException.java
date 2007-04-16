package bitronix.tm.internal;

import javax.transaction.HeuristicCommitException;

/**
 * Subclass of {@link javax.transaction.HeuristicCommitException} supporting nested {@link Throwable}s.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
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
