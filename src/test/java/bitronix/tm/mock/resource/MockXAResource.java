/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.mock.resource;

import javax.transaction.xa.*;

import bitronix.tm.internal.BitronixXAException;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.jdbc.*;

/**
 *
 * @author lorban
 */
public class MockXAResource implements XAResource {

    private int prepareRc = XAResource.XA_OK;
    private int transactiontimeout;
    private MockitoXADataSource xads;

    private XAException endException;
    private XAException prepareException;
    private XAException commitException;
    private XAException rollbackException;
    private RuntimeException prepareRuntimeException;
    private XAException recoverException;
    private long recoveryDelay;

    public MockXAResource(MockitoXADataSource xads) {
        this.xads = xads;
    }

    public void setRecoveryDelay(long recoveryDelay) {
        this.recoveryDelay = recoveryDelay;
    }

    public void setPrepareRc(int prepareRc) {
        this.prepareRc = prepareRc;
    }

    public void addInDoubtXid(Xid xid) {
        xads.addInDoubtXid(xid);
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
        boolean result = xaResource == this;
        getEventRecorder().addEvent(new XAResourceIsSameRmEvent(this, xaResource, result));
        return result;
    }

    public Xid[] recover(int flag) throws XAException {
        if (recoveryDelay > 0) {
            try {
                Thread.sleep(recoveryDelay);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (recoverException != null)
            throw recoverException;
        if (xads == null)
            return new Xid[0];
        return xads.getInDoubtXids();
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
        boolean found = xads.removeInDoubtXid(xid);
        if (!found)
            throw new BitronixXAException("unknown XID: " + xid, XAException.XAER_INVAL);
    }

    public void rollback(Xid xid) throws XAException {
        getEventRecorder().addEvent(new XAResourceRollbackEvent(this, rollbackException, xid));
        if (rollbackException != null)
            throw rollbackException;
        if (xads != null) xads.removeInDoubtXid(xid);
    }

    public void end(Xid xid, int flag) throws XAException {
        getEventRecorder().addEvent(new XAResourceEndEvent(this, xid, flag));
        if (endException != null)
            throw endException;
    }

    public void start(Xid xid, int flag) throws XAException {
        getEventRecorder().addEvent(new XAResourceStartEvent(this, xid, flag));
    }

    public void commit(Xid xid, boolean b) throws XAException {
        getEventRecorder().addEvent(new XAResourceCommitEvent(this, commitException, xid, b));
        if (commitException != null)
            throw commitException;
        if (xads != null) xads.removeInDoubtXid(xid);
    }

    public void setEndException(XAException endException) {
        this.endException = endException;
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

    public void setRecoverException(XAException recoverException) {
        this.recoverException = recoverException;
    }
}
