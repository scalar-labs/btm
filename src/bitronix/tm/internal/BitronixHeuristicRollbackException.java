package bitronix.tm.internal;

import javax.transaction.HeuristicRollbackException;

/**
 * Subclass of {@link javax.transaction.HeuristicRollbackException} supporting nested {@link Throwable}s.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BitronixHeuristicRollbackException extends HeuristicRollbackException {
    
    public BitronixHeuristicRollbackException(String string) {
        super(string);
    }

    public BitronixHeuristicRollbackException(String string, Throwable t) {
        super(string);
        initCause(t);
    }

}
