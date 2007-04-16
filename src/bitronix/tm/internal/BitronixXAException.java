package bitronix.tm.internal;

import javax.transaction.xa.XAException;

/**
 * Subclass of {@link javax.transaction.xa.XAException} supporting nested {@link Throwable}s.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixXAException extends XAException {

    public BitronixXAException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BitronixXAException(String message, int errorCode, Throwable t) {
        super(message);
        this.errorCode = errorCode;
        initCause(t);
    }

}
