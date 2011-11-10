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
package bitronix.tm.internal;

import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.BitronixXid;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.MonotonicClock;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.util.Date;

/**
 * {@link XAResourceHolder} state container.
 * Instances are kept in the transaction and bound to / unbound from the {@link XAResourceHolder} as the
 * resource participates in different transactions. A {@link XAResourceHolder} without {@link XAResourceHolderState}
 * is considered to be in local transaction mode.
 * <p>Objects of this class also expose resource specific configuration like the unique resource name.</p>
 * <p>The {@link XAResource} state during a transaction participation is also contained: assigned XID, transaction
 * start / end state...</p>
 * <p>There is exactly one {@link XAResourceHolderState} object per {@link XAResourceHolder} per
 * {@link javax.transaction.Transaction}.</p>
 *
 * @see bitronix.tm.resource.common.ResourceBean
 * @author lorban
 */
public class XAResourceHolderState {

    private final static Logger log = LoggerFactory.getLogger(XAResourceHolderState.class);

    private final ResourceBean bean;
    private final XAResourceHolder xaResourceHolder;
    private BitronixXid xid;
    private boolean started;
    private boolean ended;
    private boolean suspended;
    private Date transactionTimeoutDate;
    private boolean isTimeoutAlreadySet;
    private boolean failed;

    public XAResourceHolderState(XAResourceHolder resourceHolder, ResourceBean bean) {
        this.bean = bean;
        this.xaResourceHolder = resourceHolder;

        started = false;
        ended = false;
        suspended = false;
        isTimeoutAlreadySet = false;
        xid = null;
    }

    public XAResourceHolderState(XAResourceHolderState resourceHolderState) {
        this.bean = resourceHolderState.bean;
        this.xaResourceHolder = resourceHolderState.xaResourceHolder;

        started = false;
        ended = false;
        suspended = false;
        isTimeoutAlreadySet = false;
        xid = null;
    }

    public BitronixXid getXid() {
        return xid;
    }

    public void setXid(BitronixXid xid) throws BitronixSystemException {
        if (log.isDebugEnabled()) log.debug("assigning <" + xid + "> to <" + this + ">");
        if (this.xid != null && !xid.equals(this.xid))
            throw new BitronixSystemException("a XID has already been assigned to " + this);
        this.xid = xid;
    }

    public XAResource getXAResource() {
        return xaResourceHolder.getXAResource();
    }

    public XAResourceHolder getXAResourceHolder() {
        return xaResourceHolder;
    }

    public Date getTransactionTimeoutDate() {
        return transactionTimeoutDate;
    }

    public void setTransactionTimeoutDate(Date transactionTimeoutDate) {
        this.transactionTimeoutDate = transactionTimeoutDate;
    }

    public String getUniqueName() {
        return bean.getUniqueName();
    }

    public boolean getUseTmJoin() {
        return bean.getUseTmJoin();
    }

    public int getTwoPcOrderingPosition() {
        return bean.getTwoPcOrderingPosition();
    }

    public boolean getIgnoreRecoveryFailures() {
        return bean.getIgnoreRecoveryFailures();
    }

    public boolean isEnded() {
        return ended;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public boolean isFailed() {
        return failed;
    }

    public void end(int flags) throws XAException {
        boolean ended = this.ended;
        boolean suspended = this.suspended;

        if (this.ended && (flags == XAResource.TMSUSPEND)) {
            if (log.isDebugEnabled()) log.debug("resource already ended, changing state to suspended: " + this);
            this.suspended = true;
            return;
        }

        if (this.ended)
            throw new BitronixXAException("resource already ended: " + this, XAException.XAER_PROTO);

        if (flags == XAResource.TMSUSPEND) {
            if (!this.started)
                throw new BitronixXAException("resource hasn't been started, cannot suspend it: " + this, XAException.XAER_PROTO);
            if (this.suspended)
                throw new BitronixXAException("resource already suspended: " + this, XAException.XAER_PROTO);

            if (log.isDebugEnabled()) log.debug("suspending " + this + " with " + Decoder.decodeXAResourceFlag(flags));
            suspended = true;
        }
        else {
            if (log.isDebugEnabled()) log.debug("ending " + this + " with " + Decoder.decodeXAResourceFlag(flags));
            ended = true;
        }

        try {
            getXAResource().end(xid, flags);
            if (log.isDebugEnabled()) log.debug("ended " + this + " with " + Decoder.decodeXAResourceFlag(flags));
        } catch(XAException ex) {
            // could mean failed or unilaterally rolled back
            failed = true;
            throw ex;
        } finally {
            this.suspended = suspended;
            this.ended = ended;
            this.started = false;
        }
    }

    public void start(int flags) throws XAException {
        boolean suspended = this.suspended;
        boolean started = this.started;

        if (this.ended && (flags == XAResource.TMRESUME)) {
            if (log.isDebugEnabled()) log.debug("resource already ended, changing state to resumed: " + this);
            this.suspended = false;
            return;
        }

        if (flags == XAResource.TMRESUME) {
            if (!this.suspended)
                throw new BitronixXAException("resource hasn't been suspended, cannot resume it: " + this, XAException.XAER_PROTO);
            if (!this.started)
                throw new BitronixXAException("resource hasn't been started, cannot resume it: " + this, XAException.XAER_PROTO);

            if (log.isDebugEnabled()) log.debug("resuming " + this + " with " + Decoder.decodeXAResourceFlag(flags));
            suspended = false;
        }
        else {
            if (this.started)
                throw new BitronixXAException("resource already started: " + this, XAException.XAER_PROTO);

            if (log.isDebugEnabled()) log.debug("starting " + this + " with " + Decoder.decodeXAResourceFlag(flags));
            started = true;
        }

        if (!isTimeoutAlreadySet && transactionTimeoutDate != null && bean.getApplyTransactionTimeout()) {
            int timeoutInSeconds = (int) ((transactionTimeoutDate.getTime() - MonotonicClock.currentTimeMillis() + 999L) / 1000L);
            timeoutInSeconds = Math.max(1, timeoutInSeconds); // setting a timeout of 0 means resetting -> set it to at least 1
            if (log.isDebugEnabled()) log.debug("applying resource timeout of " + timeoutInSeconds + "s on " + this);
            getXAResource().setTransactionTimeout(timeoutInSeconds);
            isTimeoutAlreadySet = true;
        }

        getXAResource().start(xid, flags);
        this.suspended = suspended;
        this.started = started;
        this.ended = false;
        if (log.isDebugEnabled()) log.debug("started " + this + " with " + Decoder.decodeXAResourceFlag(flags));
    }

    public int hashCode() {
        return 17 * (bean.hashCode() + xid.hashCode());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof XAResourceHolderState))
            return false;

        XAResourceHolderState other = (XAResourceHolderState) obj;
        return equals(other.bean, bean) && equals(other.xid, xid);
    }

    private boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2)
            return true;
        if (obj1 == null || obj2 == null)
            return false;

        return obj1.equals(obj2);
    }

    public String toString() {
        return "an XAResourceHolderState with uniqueName=" + bean.getUniqueName() +
                " XAResource=" + getXAResource() +
                (started ? " (started)":"") +
                (ended ? " (ended)":"") +
                (suspended ? " (suspended)":"") +
                " with XID " + xid;
    }

}
