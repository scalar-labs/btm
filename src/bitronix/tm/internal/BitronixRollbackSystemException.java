package bitronix.tm.internal;

/**
 * Subclass of {@link javax.transaction.SystemException} indicating a rollback must be performed.
 * This exception is used to handle unilateral rollback of resources during delistement.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixRollbackSystemException extends BitronixSystemException {

    public BitronixRollbackSystemException(String string, Throwable t) {
        super(string, t);
    }

}
