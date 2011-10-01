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
package bitronix.tm.resource.common;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixXid;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of all services required by a {@link XAResourceHolder}. This class keeps a list of all
 * {@link XAResourceHolderState}s of the {@link XAResourceHolder} plus the currently active one. There is
 * one per transaction in which this {@link XAResourceHolder} is enlisted plus all the suspended transactions in which
 * it is enlisted as well.
 *
 * @author lorban
 */
public abstract class AbstractXAResourceHolder extends AbstractXAStatefulHolder implements XAResourceHolder {

    private final static Logger log = LoggerFactory.getLogger(AbstractXAResourceHolder.class);

    private final Map<Uid, Map<Uid, XAResourceHolderState>> xaResourceHolderStates = new HashMap<Uid, Map<Uid, XAResourceHolderState>>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    // This method is only used by tests.  It is (and always was) potentially thread-unsafe depending on what callers do with the returned map. 
    protected Map<Uid, XAResourceHolderState> getXAResourceHolderStatesForGtrid(Uid gtrid) {
        rwLock.readLock().lock();
        try {
            return xaResourceHolderStates.get(gtrid);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean isExistXAResourceHolderStatesForGtrid(Uid gtrid) {
        rwLock.readLock().lock();
        try {
            return xaResourceHolderStates.containsKey(gtrid);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getXAResourceHolderStateCountForGtrid(Uid gtrid) {
        rwLock.readLock().lock();
        try {
            Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
            if (statesForGtrid != null) {
                return statesForGtrid.size();
            }
            return 0;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void acceptVisitorForXAResourceHolderStates(Uid gtrid, XAResourceHolderStateVisitor visitor) {
        rwLock.readLock().lock();
        try {
            Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
            if (statesForGtrid != null) {
                for (XAResourceHolderState xaResourceHolderState : statesForGtrid.values()) {
                    if (!visitor.visit(xaResourceHolderState)) {
                        break;
                    }
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void putXAResourceHolderState(BitronixXid xid, XAResourceHolderState xaResourceHolderState) {
    	Uid gtrid = xid.getGlobalTransactionIdUid();
    	Uid bqual = xid.getBranchQualifierUid();
    	
    	rwLock.writeLock().lock();
        try {
        	if (log.isDebugEnabled()) { log.debug("putting XAResourceHolderState [" + xaResourceHolderState + "] on " + this); }
            if (!xaResourceHolderStates.containsKey(gtrid)) {
                if (log.isDebugEnabled()) { log.debug("GTRID [" + gtrid + "] previously unknown to " + this + ", adding it to the resource's transactions list"); }

                // use a LinkedHashMap as iteration order must be guaranteed
                Map<Uid, XAResourceHolderState> statesForGtrid = new LinkedHashMap<Uid, XAResourceHolderState>(4);
                statesForGtrid.put(bqual, xaResourceHolderState);
                xaResourceHolderStates.put(gtrid, statesForGtrid);
            }
            else {
                if (log.isDebugEnabled()) { log.debug("GTRID [" + gtrid + "] previously known to " + this + ", adding it to the resource's transactions list"); }

                Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
                statesForGtrid.put(bqual, xaResourceHolderState);
            }
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    public void removeXAResourceHolderState(BitronixXid xid) {
    	Uid gtrid = xid.getGlobalTransactionIdUid();
    	Uid bqual = xid.getBranchQualifierUid();

        rwLock.writeLock().lock();
        try {
        	if (log.isDebugEnabled()) { log.debug("removing XAResourceHolderState of xid " + xid + " from " + this); }

            Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
            if (statesForGtrid == null) {
                log.warn("tried to remove unknown GTRID [" + gtrid + "] from " + this + " - Bug?");
                return;
            }

            Object removed = statesForGtrid.remove(bqual);
            if (removed == null) {
                log.warn("tried to remove unknown BQUAL [" + bqual + "] from " + this + " - Bug?");
                return;
            }

            if (statesForGtrid.isEmpty()) {
                xaResourceHolderStates.remove(gtrid);
            }
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean hasStateForXAResource(XAResourceHolder xaResourceHolder) {
        rwLock.readLock().lock();
        try {
            for (Map<Uid, XAResourceHolderState> statesForGtrid : xaResourceHolderStates.values()) {
                for (XAResourceHolderState otherXaResourceHolderState : statesForGtrid.values()) {
                    if (otherXaResourceHolderState.getXAResource() == xaResourceHolder.getXAResource()) {
                        if (log.isDebugEnabled()) { log.debug("resource " + xaResourceHolder + " is enlisted in another transaction with " + otherXaResourceHolderState.getXid().toString()); }
                        return true;
                    }
                }
            }

            if (log.isDebugEnabled()) { log.debug("resource not enlisted in any transaction: " + xaResourceHolder); }
            return false;
        }
        finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * If this method returns false, then local transaction calls like Connection.commit() can be made.
     * @return true if start() has been successfully called but not end() yet <i>and</i> the transaction is not suspended.
     */
    public boolean isParticipatingInActiveGlobalTransaction() {
        rwLock.readLock().lock();
        try {
            BitronixTransaction currentTransaction = TransactionContextHelper.currentTransaction();
            Uid gtrid = currentTransaction == null ? null : currentTransaction.getResourceManager().getGtrid();
            if (gtrid == null)
                return false;

            Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
            if (statesForGtrid == null)
                return false;

            for (XAResourceHolderState xaResourceHolderState : statesForGtrid.values()) {
                if (xaResourceHolderState != null &&
                        xaResourceHolderState.isStarted() &&
                        !xaResourceHolderState.isSuspended() &&
                        !xaResourceHolderState.isEnded())
                    return true;
            }
            return false;
        }
        finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Simple helper method which returns a set of GTRIDs of transactions in which
     * this resource is enlisted. Useful for monitoring.
     * @return a set of String-encoded GTRIDs of transactions in which this resource is enlisted.
     */
    public Set<String> getXAResourceHolderStateGtrids() {
        rwLock.readLock().lock();
        try {
            HashSet<String> gtridsAsStrings = new HashSet<String>();

            for (Uid uid : xaResourceHolderStates.keySet()) {
                gtridsAsStrings.add(uid.toString());
            }

            return gtridsAsStrings;
        }
        finally {
            rwLock.readLock().unlock();
        }
    }
}
