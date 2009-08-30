package bitronix.tm.internal;

/**
 * {@link bitronix.tm.BitronixTransaction} status change listener interface.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public interface TransactionStatusChangeListener {

    /**
     * Fired when the status of a {@link bitronix.tm.BitronixTransaction} has changed.
     * @param oldStatus the old status of the {@link bitronix.tm.BitronixTransaction}.
     * @param newStatus the new status of the {@link bitronix.tm.BitronixTransaction}.
     * @see javax.transaction.Status Status constant values.
     */
    public void statusChanged(int oldStatus, int newStatus);

}
