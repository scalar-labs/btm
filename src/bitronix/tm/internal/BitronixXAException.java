package bitronix.tm.internal;

import javax.transaction.xa.XAException;

/**
 * Subclass of {@link javax.transaction.xa.XAException} supporting nested {@link Throwable}s.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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

    public static boolean isUnilateralRollback(XAException ex) {
        return (ex.errorCode >= XAException.XA_RBBASE && ex.errorCode <= XAException.XA_RBEND) || ex.errorCode == XAException.XAER_NOTA;
    }
    
}
