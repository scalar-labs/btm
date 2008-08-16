package bitronix.tm.internal;

/**
 * Subclass of {@link javax.transaction.SystemException} indicating a rollback must be performed.
 * This exception is used to handle unilateral rollback of resources during delistement.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BitronixRollbackSystemException extends BitronixSystemException {

    public BitronixRollbackSystemException(String string, Throwable t) {
        super(string, t);
    }

}
