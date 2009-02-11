package bitronix.tm.internal;

import javax.transaction.RollbackException;

/**
 * Subclass of {@link javax.transaction.RollbackException} supporting nested {@link Throwable}s.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BitronixRollbackException extends RollbackException {

    public BitronixRollbackException(String string) {
        super(string);
    }

    public BitronixRollbackException(String string, Throwable t) {
        super(string);
        initCause(t);
    }

}
