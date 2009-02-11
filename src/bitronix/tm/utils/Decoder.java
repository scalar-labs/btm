package bitronix.tm.utils;

import bitronix.tm.journal.TransactionLogHeader;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.internal.XAResourceHolderState;

import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.util.Collection;
import java.util.Iterator;

/**
 * Constant to string decoder.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class Decoder {

    public static String decodeXAExceptionErrorCode(XAException ex) {
        switch (ex.errorCode) {
            // rollback errors
            case XAException.XA_RBROLLBACK: return "XA_RBROLLBACK";
            case XAException.XA_RBCOMMFAIL: return "XA_RBCOMMFAIL";
            case XAException.XA_RBDEADLOCK: return "XA_RBDEADLOCK";
            case XAException.XA_RBTRANSIENT: return "XA_RBTRANSIENT";
            case XAException.XA_RBINTEGRITY: return "XA_RBINTEGRITY";
            case XAException.XA_RBOTHER: return "XA_RBOTHER";
            case XAException.XA_RBPROTO: return "XA_RBPROTO";
            case XAException.XA_RBTIMEOUT: return "XA_RBTIMEOUT";

            // heuristic errors
            case XAException.XA_HEURCOM: return "XA_HEURCOM";
            case XAException.XA_HEURHAZ: return "XA_HEURHAZ";
            case XAException.XA_HEURMIX: return "XA_HEURMIX";
            case XAException.XA_HEURRB: return "XA_HEURRB";

            // misc failures errors
            case XAException.XAER_RMERR: return "XAER_RMERR";
            case XAException.XAER_RMFAIL: return "XAER_RMFAIL";
            case XAException.XAER_NOTA: return "XAER_NOTA";
            case XAException.XAER_INVAL: return "XAER_INVAL";
            case XAException.XAER_PROTO: return "XAER_PROTO";
            case XAException.XAER_ASYNC: return "XAER_ASYNC";
            case XAException.XAER_DUPID: return "XAER_DUPID";
            case XAException.XAER_OUTSIDE: return "XAER_OUTSIDE";

            default: return "!invalid error code (" + ex.errorCode + ")!";
        }
    }

    public static String decodeStatus(int status) {
        switch (status) {
            case Status.STATUS_ACTIVE: return "ACTIVE";
            case Status.STATUS_COMMITTED: return "COMMITTED";
            case Status.STATUS_COMMITTING: return "COMMITTING";
            case Status.STATUS_MARKED_ROLLBACK: return "MARKED_ROLLBACK";
            case Status.STATUS_NO_TRANSACTION: return "NO_TRANSACTION";
            case Status.STATUS_PREPARED: return "PREPARED";
            case Status.STATUS_PREPARING: return "PREPARING";
            case Status.STATUS_ROLLEDBACK: return "ROLLEDBACK";
            case Status.STATUS_ROLLING_BACK: return "ROLLING_BACK";
            case Status.STATUS_UNKNOWN: return "UNKNOWN";
            default: return "!incorrect status (" + status + ")!";
        }
    }

    public static String decodeXAResourceFlag(int flag) {
        switch (flag) {
            case XAResource.TMENDRSCAN: return "ENDRSCAN";
            case XAResource.TMFAIL: return "FAIL";
            case XAResource.TMJOIN: return "JOIN";
            case XAResource.TMNOFLAGS: return "NOFLAGS";
            case XAResource.TMONEPHASE: return "ONEPHASE";
            case XAResource.TMRESUME: return "RESUME";
            case XAResource.TMSTARTRSCAN: return "STARTRSCAN";
            case XAResource.TMSUCCESS: return "SUCCESS";
            case XAResource.TMSUSPEND: return "SUSPEND";
            default: return "!invalid flag (" + flag + ")!";
        }
    }

    public static String decodePrepareVote(int vote) {
        switch (vote) {
            case XAResource.XA_OK: return "XA_OK";
            case XAResource.XA_RDONLY: return "XA_RDONLY";
            default: return "!invalid return code (" + vote + ")!";
        }
    }

    public static String decodeHeaderState(byte state) {
        switch (state) {
            case TransactionLogHeader.CLEAN_LOG_STATE: return "CLEAN_LOG_STATE";
            case TransactionLogHeader.UNCLEAN_LOG_STATE: return "UNCLEAN_LOG_STATE";
            default: return "!invalid state (" + state + ")!";
        }
    }

    public static String decodeXAStatefulHolderState(int state) {
        switch (state) {
            case XAStatefulHolder.STATE_CLOSED: return "CLOSED";
            case XAStatefulHolder.STATE_IN_POOL: return "IN_POOL";
            case XAStatefulHolder.STATE_ACCESSIBLE: return "ACCESSIBLE";
            case XAStatefulHolder.STATE_NOT_ACCESSIBLE: return "NOT_ACCESSIBLE";
            default: return "!invalid state (" + state + ")!";
        }
    }

    /**
     * Create a String representation of a list of {@link bitronix.tm.resource.common.XAResourceHolder}s. This
     * String will contain each resource's unique name.
     * @param resources a list of {@link bitronix.tm.resource.common.XAResourceHolder}s.
     * @return a String representation of the list.
     */
    public static String collectResourcesNames(Collection resources) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");

        Iterator it = resources.iterator();
        while (it.hasNext()) {
            XAResourceHolderState resourceHolderState = (XAResourceHolderState) it.next();
            sb.append(resourceHolderState.getUniqueName());

            if (it.hasNext())
                sb.append(", ");
        }

        sb.append("]");
        return sb.toString();
    }

}
