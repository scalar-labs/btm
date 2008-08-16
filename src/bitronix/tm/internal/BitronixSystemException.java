package bitronix.tm.internal;

import javax.transaction.SystemException;

/**
 * Subclass of {@link javax.transaction.SystemException} supporting nested {@link Throwable}s.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
