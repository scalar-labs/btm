package bitronix.tm.internal;

import javax.transaction.RollbackException;

/**
 * Subclass of {@link javax.transaction.RollbackException} supporting nested {@link Throwable}s.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
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
