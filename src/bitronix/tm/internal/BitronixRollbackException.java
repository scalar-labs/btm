package bitronix.tm.internal;

import javax.transaction.RollbackException;
import java.util.List;

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

    public static String buildResourceNamesString(List rolledBackResources) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");

        for (int i = 0; i < rolledBackResources.size(); i++) {
            XAResourceHolderState holderState = (XAResourceHolderState) rolledBackResources.get(i);
            sb.append(holderState.getUniqueName());

            if (i < rolledBackResources.size() -1)
                sb.append(", ");
        }

        sb.append("]");
        return sb.toString();
    }


}
