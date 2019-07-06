/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.internal;

import bitronix.tm.BitronixXid;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.MonotonicClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author Ludovic Orban
 */
public class XAResourceHolderState {

    private final static Logger log = LoggerFactory.getLogger(XAResourceHolderState.class);

    private final ResourceBean bean;
    private final XAResourceHolder xaResourceHolder;
    private volatile BitronixXid xid;
    private volatile boolean started;
    private volatile boolean ended;
    private volatile boolean suspended;
    private volatile Date transactionTimeoutDate;
    private volatile boolean isTimeoutAlreadySet;
    private volatile boolean failed;
    private volatile int hashCode;

    public XAResourceHolderState(XAResourceHolder resourceHolder, ResourceBean bean) {
        this.bean = bean;
        this.xaResourceHolder = resourceHolder;

        started = false;
        ended = false;
        suspended = false;
        isTimeoutAlreadySet = false;
        xid = null;
        hashCode = 17 * bean.hashCode();
    }

    public XAResourceHolderState(XAResourceHolderState resourceHolderState) {
        this.bean = resourceHolderState.bean;
        this.xaResourceHolder = resourceHolderState.xaResourceHolder;

        started = false;
        ended = false;
        suspended = false;
        isTimeoutAlreadySet = false;
        xid = null;
        hashCode = 17 * bean.hashCode();
    }

    public BitronixXid getXid() {
        return xid;
    }

    public void setXid(BitronixXid xid) throws BitronixSystemException {
        if (log.isDebugEnabled()) { log.debug("assigning <" + xid + "> to <" + this + ">"); }
        if (this.xid != null && !xid.equals(this.xid))
            throw new BitronixSystemException("a XID has already been assigned to " + this);
        this.xid = xid;
        hashCode = 17 * (bean.hashCode() + (xid != null ? xid.hashCode() : 0));
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
    
    public void quickSuspend() throws XAException {
        if (!this.started) {
            throw new BitronixXAException("resource hasn't been started, cannot suspend it: " + this, XAException.XAER_PROTO);
        }
        if (this.suspended) {
            throw new BitronixXAException("resource already suspended: " + this, XAException.XAER_PROTO);
        }

        if (log.isDebugEnabled()) { log.debug("quick suspending " + this); }

        //take effect
        this.suspended = true;
    }

    public void quickResume() throws XAException {
       if (!this.started) {
          throw new BitronixXAException("resource hasn't been started, cannot quick-resume it: " + this, XAException.XAER_PROTO);
       }
       if (!this.suspended) {
           throw new BitronixXAException("resource hasn't been suspended, cannot quick resume it: " + this, XAException.XAER_PROTO);
       }

       if (log.isDebugEnabled()) { log.debug("quick resuming " + this); }

       //take effect
       this.suspended = false;
   }
    
    public void end(int flags) throws XAException {
        boolean ended = this.ended;
        boolean suspended = this.suspended;

        if (this.ended && (flags == XAResource.TMSUSPEND)) {
            if (log.isDebugEnabled()) { log.debug("resource already ended, changing state to suspended: " + this); }
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

            if (log.isDebugEnabled()) { log.debug("suspending " + this + " with " + Decoder.decodeXAResourceFlag(flags)); }
            suspended = true;
        }
        else {
            if (log.isDebugEnabled()) { log.debug("ending " + this + " with " + Decoder.decodeXAResourceFlag(flags)); }
            ended = true;
        }

        try {
            getXAResource().end(xid, flags);
            if (log.isDebugEnabled()) { log.debug("ended " + this + " with " + Decoder.decodeXAResourceFlag(flags)); }
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
            if (log.isDebugEnabled()) { log.debug("resource already ended, changing state to resumed: " + this); }
            this.suspended = false;
            return;
        }

        if (flags == XAResource.TMRESUME) {
            if (!this.suspended)
                throw new BitronixXAException("resource hasn't been suspended, cannot resume it: " + this, XAException.XAER_PROTO);
            if (!this.started)
                throw new BitronixXAException("resource hasn't been started, cannot resume it: " + this, XAException.XAER_PROTO);

            if (log.isDebugEnabled()) { log.debug("resuming " + this + " with " + Decoder.decodeXAResourceFlag(flags)); }
            suspended = false;
        }
        else {
            if (this.started)
                throw new BitronixXAException("resource already started: " + this, XAException.XAER_PROTO);

            if (log.isDebugEnabled()) { log.debug("starting " + this + " with " + Decoder.decodeXAResourceFlag(flags)); }
            started = true;
        }

        if (!isTimeoutAlreadySet && transactionTimeoutDate != null && bean.getApplyTransactionTimeout()) {
            int timeoutInSeconds = (int) ((transactionTimeoutDate.getTime() - MonotonicClock.currentTimeMillis() + 999L) / 1000L);
            timeoutInSeconds = Math.max(1, timeoutInSeconds); // setting a timeout of 0 means resetting -> set it to at least 1
            if (log.isDebugEnabled()) { log.debug("applying resource timeout of " + timeoutInSeconds + "s on " + this); }
            getXAResource().setTransactionTimeout(timeoutInSeconds);
            isTimeoutAlreadySet = true;
        }

        getXAResource().start(xid, flags);
        this.suspended = suspended;
        this.started = started;
        this.ended = false;
        if (log.isDebugEnabled()) { log.debug("started " + this + " with " + Decoder.decodeXAResourceFlag(flags)); }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof XAResourceHolderState) || this.hashCode != obj.hashCode())
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

    @Override
    public String toString() {
        return "an XAResourceHolderState with uniqueName=" + bean.getUniqueName() +
                " XAResource=" + getXAResource() +
                (started ? " (started)":"") +
                (ended ? " (ended)":"") +
                (suspended ? " (suspended)":"") +
                " with XID " + xid;
    }
}
