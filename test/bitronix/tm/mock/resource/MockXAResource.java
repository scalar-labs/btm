package bitronix.tm.mock.resource;

import bitronix.tm.mock.events.*;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class MockXAResource implements XAResource {

    private int prepareRc = XAResource.XA_OK;
    private int transactiontimeout;

    private XAException prepareException;
    private XAException commitException;
    private XAException rollbackException;
    private RuntimeException prepareRuntimeException;

    public MockXAResource() {
    }


    public void setPrepareRc(int prepareRc) {
        this.prepareRc = prepareRc;
    }

    private EventRecorder getEventRecorder() {
        return EventRecorder.getEventRecorder(this);
    }

    /*
    Interface implementation
    */

    public int getTransactionTimeout() throws XAException {
        return transactiontimeout;
    }

    public boolean setTransactionTimeout(int i) throws XAException {
        this.transactiontimeout = i;
        return true;
    }

    public boolean isSameRM(XAResource xaResource) throws XAException {
        return false;
    }

    public Xid[] recover(int flag) throws XAException {
        return new Xid[0];
    }

    public int prepare(Xid xid) throws XAException {
        if (prepareException != null) {
            getEventRecorder().addEvent(new XAResourcePrepareEvent(this, prepareException, xid, -1));
            prepareException.fillInStackTrace();
            throw prepareException;
        }
        if (prepareRuntimeException != null) {
            prepareRuntimeException.fillInStackTrace();
            getEventRecorder().addEvent(new XAResourcePrepareEvent(this, prepareRuntimeException, xid, -1));
            throw prepareRuntimeException;
        }
        getEventRecorder().addEvent(new XAResourcePrepareEvent(this, xid, prepareRc));
        return prepareRc;
    }

    public void forget(Xid xid) throws XAException {
        getEventRecorder().addEvent(new XAResourceForgetEvent(this, xid));
    }

    public void rollback(Xid xid) throws XAException {
        getEventRecorder().addEvent(new XAResourceRollbackEvent(this, rollbackException, xid));
        if (rollbackException != null)
            throw rollbackException;
    }

    public void end(Xid xid, int flag) throws XAException {
        getEventRecorder().addEvent(new XAResourceEndEvent(this, xid, flag));
    }

    public void start(Xid xid, int flag) throws XAException {
        getEventRecorder().addEvent(new XAResourceStartEvent(this, xid, flag));
    }

    public void commit(Xid xid, boolean b) throws XAException {
        getEventRecorder().addEvent(new XAResourceCommitEvent(this, commitException, xid, b));
        if (commitException != null)
            throw commitException;
    }

    public void setPrepareException(XAException prepareException) {
        this.prepareException = prepareException;
    }

    public void setPrepareException(RuntimeException prepareException) {
        this.prepareRuntimeException = prepareException;
    }

    public void setCommitException(XAException commitException) {
        this.commitException = commitException;
    }

    public void setRollbackException(XAException rollbackException) {
        this.rollbackException = rollbackException;
    }
}
