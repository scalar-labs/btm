package bitronix.tm.internal;

import javax.transaction.SystemException;

/**
 * Subclass of {@link javax.transaction.SystemException} supporting nested {@link Throwable}s.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixSystemException extends SystemException {

    public BitronixSystemException(int errorCode) {
        super(errorCode);
    }

    public BitronixSystemException(String string) {
        super(string);
    }

    public BitronixSystemException(String string, Throwable t) {
        super(string);
        initCause(t);
    }

}
