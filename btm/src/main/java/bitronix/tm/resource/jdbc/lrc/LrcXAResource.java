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
package bitronix.tm.resource.jdbc.lrc;

import bitronix.tm.internal.BitronixXAException;
import bitronix.tm.utils.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * XAResource implementation for a non-XA JDBC connection emulating XA with Last Resource Commit.
 * <p>The XA protocol flow is implemented by this state machine:</p>
 * <pre>
 * NO_TX
 *   |
 *   | start(TMNOFLAGS)
 *   |
 *   |       end(TMFAIL)
 * STARTED -------------- NO_TX
 *   |
 *   | end(TMSUCCESS)
 *   |
 *   |    start(TMJOIN)
 * ENDED ---------------- STARTED
 *   |\
 *   | \  commit (one phase)
 *   |  ----------------- NO_TX
 *   |
 *   | prepare()
 *   |
 *   |       commit() or
 *   |       rollback()
 * PREPARED ------------- NO_TX
 * </pre>
 * {@link XAResource#TMSUSPEND} and {@link XAResource#TMRESUME} are not supported.
 *
 * @author lorban
 */
public class LrcXAResource implements XAResource {

    private final static Logger log = LoggerFactory.getLogger(LrcXAResource.class);

    public static final int NO_TX = 0;
    public static final int STARTED = 1;
    public static final int ENDED = 2;
    public static final int PREPARED = 3;

    private final Connection connection;
    private volatile Xid xid;
    private volatile boolean autocommitActiveBeforeStart;
    private volatile int state = NO_TX;

    public LrcXAResource(Connection connection) {
        this.connection = connection;
    }


    public int getState() {
        return state;
    }

    private String xlatedState() {
        switch (state) {
            case NO_TX: return "NO_TX";
            case STARTED: return "STARTED";
            case ENDED: return "ENDED";
            case PREPARED: return "PREPARED";
            default: return "!invalid state (" + state + ")!";
        }
    }

    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return false;
    }

    public void forget(Xid xid) throws XAException {
    }

    public Xid[] recover(int flags) throws XAException {
        return new Xid[0];
    }

    public boolean isSameRM(XAResource xaResource) throws XAException {
        return xaResource == this;
    }

    public void start(Xid xid, int flag) throws XAException {
        if (flag != XAResource.TMNOFLAGS  && flag != XAResource.TMJOIN)
            throw new BitronixXAException("unsupported start flag " + Decoder.decodeXAResourceFlag(flag), XAException.XAER_RMERR);
        if (xid == null)
            throw new BitronixXAException("XID cannot be null", XAException.XAER_INVAL);

        if (state == NO_TX) {
            if (this.xid != null)
                throw new BitronixXAException("resource already started on XID " + this.xid, XAException.XAER_PROTO);
            else {
                if (flag == XAResource.TMJOIN)
                    throw new BitronixXAException("resource not yet started", XAException.XAER_PROTO);
                else {
                    if (log.isDebugEnabled()) log.debug("OK to start, old state=" + xlatedState() + ", XID=" + xid + ", flag=" + Decoder.decodeXAResourceFlag(flag));
                    this.xid = xid;
                }
            }
        }
        else if (state == STARTED) {
            throw new BitronixXAException("resource already started on XID " + this.xid, XAException.XAER_PROTO);
        }
        else if (state == ENDED) {
            if (flag == XAResource.TMNOFLAGS)
                throw new BitronixXAException("resource already registered XID " + this.xid, XAException.XAER_DUPID);
            else {
                if (xid.equals(this.xid)) {
                    if (log.isDebugEnabled()) log.debug("OK to join, old state=" + xlatedState() + ", XID=" + xid + ", flag=" + Decoder.decodeXAResourceFlag(flag));
                }
                else
                    throw new BitronixXAException("resource already started on XID " + this.xid + " - cannot start it on more than one XID at a time", XAException.XAER_RMERR);
            }
        }
        else if (state == PREPARED) {
            throw new BitronixXAException("resource already prepared on XID " + this.xid, XAException.XAER_PROTO);
        }

        try {
            autocommitActiveBeforeStart = connection.getAutoCommit();
            if (autocommitActiveBeforeStart) {
                if (log.isDebugEnabled()) log.debug("disabling autocommit mode on non-XA connection");
                connection.setAutoCommit(false);
            }
            this.state = STARTED;
        } catch (SQLException ex) {
            throw new BitronixXAException("cannot disable autocommit on non-XA connection", XAException.XAER_RMERR);
        }
    }

    public void end(Xid xid, int flag) throws XAException {
        if (flag != XAResource.TMSUCCESS && flag != XAResource.TMFAIL)
            throw new BitronixXAException("unsupported end flag " + Decoder.decodeXAResourceFlag(flag), XAException.XAER_RMERR);
        if (xid == null)
            throw new BitronixXAException("XID cannot be null", XAException.XAER_INVAL);

        if (state == NO_TX) {
            throw new BitronixXAException("resource never started on XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == STARTED) {
            if (this.xid.equals(xid)) {
                if (log.isDebugEnabled()) log.debug("OK to end, old state=" + xlatedState() + ", XID=" + xid + ", flag=" + Decoder.decodeXAResourceFlag(flag));
            }
            else
                throw new BitronixXAException("resource already started on XID " + this.xid + " - cannot end it on another XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == ENDED) {
            throw new BitronixXAException("resource already ended on XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == PREPARED) {
            throw new BitronixXAException("cannot end, resource already prepared on XID " + xid, XAException.XAER_PROTO);
        }

        if (flag == XAResource.TMFAIL) {
            try {
                connection.rollback();
                state = NO_TX;
                this.xid = null;
                return;
            } catch (SQLException ex) {
                throw new BitronixXAException("error rolling back resource on end", XAException.XAER_RMERR, ex);
            }
        }

        this.state = ENDED;
    }

    public int prepare(Xid xid) throws XAException {
        if (xid == null)
            throw new BitronixXAException("XID cannot be null", XAException.XAER_INVAL);

        if (state == NO_TX) {
            throw new BitronixXAException("resource never started on XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == STARTED) {
            throw new BitronixXAException("resource never ended on XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == ENDED) {
            if (this.xid.equals(xid)) {
                if (log.isDebugEnabled()) log.debug("OK to prepare, old state=" + xlatedState() + ", XID=" + xid);
            }
            else
                throw new BitronixXAException("resource already started on XID " + this.xid + " - cannot prepare it on another XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == PREPARED) {
            throw new BitronixXAException("resource already prepared on XID " + this.xid, XAException.XAER_PROTO);
        }
        
        try {
            connection.commit();
            this.state = PREPARED;
            return XAResource.XA_OK;
        } catch (SQLException ex) {
            throw new BitronixXAException("error preparing non-XA resource", XAException.XAER_RMERR, ex);
        }
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (xid == null)
            throw new BitronixXAException("XID cannot be null", XAException.XAER_INVAL);

        if (state == NO_TX) {
            throw new BitronixXAException("resource never started on XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == STARTED) {
            throw new BitronixXAException("resource never ended on XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == ENDED) {
            if (onePhase) {
                if (log.isDebugEnabled()) log.debug("OK to commit with 1PC, old state=" + xlatedState() + ", XID=" + xid);
                try {
                    connection.commit();
                } catch (SQLException ex) {
                    throw new BitronixXAException("error committing (one phase) non-XA resource", XAException.XAER_RMERR, ex);
                }
            }
            else
                throw new BitronixXAException("resource never prepared on XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == PREPARED) {
            if (!onePhase) {
                if (this.xid.equals(xid)) {
                    if (log.isDebugEnabled()) log.debug("OK to commit, old state=" + xlatedState() + ", XID=" + xid);
                }
                else
                    throw new BitronixXAException("resource already started on XID " + this.xid + " - cannot commit it on another XID " + xid, XAException.XAER_PROTO);
            }
            else
                throw new BitronixXAException("cannot commit in one phase as resource has been prepared on XID " + xid, XAException.XAER_PROTO);
        }

        this.state = NO_TX;
        this.xid = null;

        try {
            if (autocommitActiveBeforeStart) {
                if (log.isDebugEnabled()) log.debug("enabling back autocommit mode on non-XA connection");
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new BitronixXAException("cannot reset autocommit on non-XA connection", XAException.XAER_RMERR);
        }
    }

    public void rollback(Xid xid) throws XAException {
        if (xid == null)
            throw new BitronixXAException("XID cannot be null", XAException.XAER_INVAL);

        if (state == NO_TX) {
            throw new BitronixXAException("resource never started on XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == STARTED) {
            throw new BitronixXAException("resource never ended on XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == ENDED) {
            if (this.xid.equals(xid)) {
                if (log.isDebugEnabled()) log.debug("OK to rollback, old state=" + xlatedState() + ", XID=" + xid);
            }
            else
                throw new BitronixXAException("resource already started on XID " + this.xid + " - cannot roll it back on another XID " + xid, XAException.XAER_PROTO);
        }
        else if (state == PREPARED) {
            this.state = NO_TX;
            throw new BitronixXAException("resource committed during prepare on XID " + this.xid, XAException.XA_HEURCOM);
        }

        try {
            connection.rollback();
        } catch (SQLException ex) {
            throw new BitronixXAException("error preparing non-XA resource", XAException.XAER_RMERR, ex);
        } finally {
            this.state = NO_TX;
            this.xid = null;
        }

        try {
            if (autocommitActiveBeforeStart) {
                if (log.isDebugEnabled()) log.debug("enabling back autocommit mode on non-XA connection");
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new BitronixXAException("cannot reset autocommit on non-XA connection", XAException.XAER_RMERR);
        }
    }

    public String toString() {
        return "a JDBC LrcXAResource in state " + xlatedState();
    }
}
