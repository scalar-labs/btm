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

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.utils.Scheduler;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.util.*;

/**
 * Every {@link bitronix.tm.BitronixTransaction} contains an instance of this class that is used to register
 * and keep track of resources enlisted in a transaction.
 *
 * @author lorban
 */
public class XAResourceManager {

    private final static Logger log = LoggerFactory.getLogger(XAResourceManager.class);

    private final Uid gtrid;
    private final Scheduler<XAResourceHolderState> resources = new Scheduler<XAResourceHolderState>();

    /**
     * Create a resource manager for the specified GTRID.
     * @param gtrid the transaction's GTRID this XAResourceManager will be assigned to.
     */
    public XAResourceManager(Uid gtrid) {
        this.gtrid = gtrid;
    }

    /**
     * Enlist the specified {@link XAResourceHolderState}. A XID is generated and the resource is started with
     * XAResource.TMNOFLAGS or XAResource.TMJOIN if it could be joined with another previously enlisted one.
     * <br/>
     * There are 3 different cases that can happen when a {@link XAResourceHolderState} is enlisted:
     * <ul>
     * <li>already enlisted and not ended: do nothing</li>
     * <li>already enlisted and ended: try to join. if you can join, keep a reference on the passed-in
     *     {@link XAResourceHolderState} and drop the previous one. if you cannot join, it's the same as case 3</li>
     * <li>not enlisted: create a new branch and keep a reference on the passed-in {@link XAResourceHolderState}</li>
     * </ul>
     *
     * @param xaResourceHolderState the {@link XAResourceHolderState} to be enlisted.
     * @throws XAException if a resource error occured.
     * @throws BitronixSystemException if an internal error occured.
     */
    public void enlist(XAResourceHolderState xaResourceHolderState) throws XAException, BitronixSystemException {
        XAResourceHolderState alreadyEnlistedHolder = findXAResourceHolderState(xaResourceHolderState.getXAResource());
        if (alreadyEnlistedHolder != null && !alreadyEnlistedHolder.isEnded()) {
            xaResourceHolderState.setXid(alreadyEnlistedHolder.getXid());
            log.warn("ignoring enlistment of already enlisted but not ended resource " + xaResourceHolderState);
            return;
        }

        XAResourceHolderState toBeJoinedHolderState = null;
        if (alreadyEnlistedHolder != null) {
            if (log.isDebugEnabled()) log.debug("resource already enlisted but has been ended eligible for join: " + alreadyEnlistedHolder);
            toBeJoinedHolderState = getManagedResourceWithSameRM(xaResourceHolderState);
        }

        BitronixXid xid;
        int flag;

        if (toBeJoinedHolderState != null) {
            if (log.isDebugEnabled()) log.debug("joining " + xaResourceHolderState + " with " + toBeJoinedHolderState);
            xid = toBeJoinedHolderState.getXid();
            flag = XAResource.TMJOIN;
        }
        else {
            xid = UidGenerator.generateXid(gtrid);
            if (log.isDebugEnabled()) log.debug("creating new branch with " + xid);
            flag = XAResource.TMNOFLAGS;
        }

        // check for enlistment of a 2nd LRC resource, forbid this if the 2nd resource cannot be joined with the 1st one
        // unless this is explicitly allowed in the config
        if (flag != XAResource.TMJOIN && xaResourceHolderState.getTwoPcOrderingPosition() == Scheduler.ALWAYS_LAST_POSITION &&
                !TransactionManagerServices.getConfiguration().isAllowMultipleLrc()) {
            List<XAResourceHolderState> alwaysLastResources = resources.getByNaturalOrderForPosition(Scheduler.ALWAYS_LAST_POSITION);
            if (alwaysLastResources != null && !alwaysLastResources.isEmpty())
                throw new BitronixSystemException("cannot enlist more than one non-XA resource, tried enlisting " + xaResourceHolderState + ", already enlisted: " + alwaysLastResources.get(0));
        }

        xaResourceHolderState.setXid(xid);
        xaResourceHolderState.start(flag);


        // in case of a JOIN, the resource holder is already in the scheduler -> do not add it twice
        if (toBeJoinedHolderState != null) {
            resources.remove(toBeJoinedHolderState);
        }
        // this must be done only after start() successfully returned
        resources.add(xaResourceHolderState, xaResourceHolderState.getTwoPcOrderingPosition());
    }

    /**
     * Delist the specified {@link XAResourceHolderState}. A reference to the resource is kept anyway.
     * @param xaResourceHolderState the {@link XAResourceHolderState} to be delisted.
     * @param flag the delistment flag.
     * @return true if the resource could be delisted, false otherwise.
     * @throws XAException if the resource threw an exception during delistment.
     * @throws BitronixSystemException if an internal error occured.
     */
    public boolean delist(XAResourceHolderState xaResourceHolderState, int flag) throws XAException, BitronixSystemException {
        if (findXAResourceHolderState(xaResourceHolderState.getXAResource()) != null) {
            if (log.isDebugEnabled()) log.debug("delisting resource " + xaResourceHolderState);
            xaResourceHolderState.end(flag);
            return true;
        }

        log.warn("trying to delist resource that has not been previously enlisted: " + xaResourceHolderState);
        return false;
    }

    /**
     * Suspend all enlisted resources from the current transaction context.
     * @throws XAException if the resource threw an exception during suspend.
     */
    public void suspend() throws XAException {
        for (XAResourceHolderState xaResourceHolderState : resources) {
            if (!xaResourceHolderState.isEnded()) {
                if (log.isDebugEnabled()) log.debug("suspending " + xaResourceHolderState);
                xaResourceHolderState.end(XAResource.TMSUCCESS);
            }
        } // while
    }

    /**
     * Resume all enlisted resources in the current transaction context.
     * @throws XAException if the resource threw an exception during resume.
     */
    public void resume() throws XAException {
        // all XAResource needs to be re-enlisted but this must happen
        // outside the Scheduler's iteration as enlist() can change the
        // collection's content and confuse the iterator.
        List<XAResourceHolderState> toBeReEnlisted = new ArrayList<XAResourceHolderState>();

        for (XAResourceHolderState xaResourceHolderState : resources) {
            if (log.isDebugEnabled()) log.debug("resuming " + xaResourceHolderState);

            // If a prepared statement is (re-)used after suspend/resume is performed its XAResource needs to be
            // re-enlisted. This must be done outside this loop or that will confuse the iterator!
            toBeReEnlisted.add(new XAResourceHolderState(xaResourceHolderState));
        }

        if (toBeReEnlisted.size() > 0 && log.isDebugEnabled()) log.debug("re-enlisting " + toBeReEnlisted.size() + " resource(s)");
        for (XAResourceHolderState xaResourceHolderState : toBeReEnlisted) {
            if (log.isDebugEnabled()) log.debug("re-enlisting resource " + xaResourceHolderState);
            try {
                enlist(xaResourceHolderState);
                xaResourceHolderState.getXAResourceHolder().putXAResourceHolderState(xaResourceHolderState.getXid(), xaResourceHolderState);
            } catch (BitronixSystemException ex) {
                throw new BitronixXAException("error re-enlisting resource during resume: " + xaResourceHolderState, XAException.XAER_RMERR, ex);
            }
        }
    }

    /**
     * Look if an {@link XAResource} has already been enlisted.
     * @param xaResource the {@link XAResource} to look for.
     * @return the {@link XAResourceHolderState} of the enlisted resource or null if the {@link XAResource} has not
     *         been enlisted in this {@link XAResourceManager}.
     * @throws BitronixSystemException if an internal error happens.
     */
    public XAResourceHolderState findXAResourceHolderState(XAResource xaResource) throws BitronixSystemException {
        for (XAResourceHolderState xaResourceHolderState : resources) {
            if (xaResourceHolderState.getXAResource() == xaResource)
                return xaResourceHolderState;
        }
        
        return null;
    }

    /**
     * Search for an eventually already enlisted {@link XAResourceHolderState} that could be joined with the
     * {@link XAResourceHolderState} passed as parameter.<br/>
     * If datasource configuration property <code>bitronix.useTmJoin=false</code> is set this method always returns null.
     * @param xaResourceHolderState a {@link XAResourceHolderState} looking to be joined.
     * @return another enlisted {@link XAResourceHolderState} that can be joined with the one passed in or null if none is found.
     * @throws XAException if call to XAResource.isSameRM() fails.
     */
    private XAResourceHolderState getManagedResourceWithSameRM(XAResourceHolderState xaResourceHolderState) throws XAException {
        if (!xaResourceHolderState.getUseTmJoin()) {
            if (log.isDebugEnabled()) log.debug("join disabled on resource " + xaResourceHolderState);
            return null;
        }

        for (XAResourceHolderState alreadyEnlistedHolderState : resources) {
            if (log.isDebugEnabled())
                log.debug("checking joinability of " + xaResourceHolderState + " with " + alreadyEnlistedHolderState);
            if (alreadyEnlistedHolderState.isEnded() &&
                    !alreadyEnlistedHolderState.isSuspended() &&
                    xaResourceHolderState.getXAResource().isSameRM(alreadyEnlistedHolderState.getXAResource())) {
                if (log.isDebugEnabled()) log.debug("resources are joinable");
                return alreadyEnlistedHolderState;
            }
            if (log.isDebugEnabled()) log.debug("resources are not joinable");
        }
        
        if (log.isDebugEnabled()) log.debug("no joinable resource found for " + xaResourceHolderState);
        return null;
    }

    /**
     * Remove this transaction's {@link XAResourceHolderState} from all enlisted
     * {@link bitronix.tm.resource.common.XAResourceHolder}s.
     */
    public void clearXAResourceHolderStates() {
        if (log.isDebugEnabled()) log.debug("clearing XAResourceHolder states on " + resources.size() + " resource(s)");
        Iterator<XAResourceHolderState> it = resources.iterator();
        while (it.hasNext()) {
            XAResourceHolderState xaResourceHolderState = it.next();
            XAResourceHolder resourceHolder = xaResourceHolderState.getXAResourceHolder();

            // clear out the current state
            resourceHolder.removeXAResourceHolderState(xaResourceHolderState.getXid());

            Map statesForGtrid = resourceHolder.getXAResourceHolderStatesForGtrid(gtrid);
            if (statesForGtrid != null) log.warn("resource " + resourceHolder + " did not clean up " + statesForGtrid.size() + "transaction states for GTRID [" + gtrid + "]");
            else if (log.isDebugEnabled()) log.debug("resource " + resourceHolder + " cleaned up all transaction states for GTRID [" + gtrid + "]");

            it.remove();
        }
    }

    /**
     * Get a {@link Set} of unique names of all the enlisted {@link XAResourceHolderState}s.
     * @return a {@link Set} of unique names of all the enlisted {@link XAResourceHolderState}s.
     */
    public Set<String> collectUniqueNames() {
        Set<String> names = new HashSet<String>(resources.size());
        for (XAResourceHolderState xaResourceHolderState : resources) {
            names.add(xaResourceHolderState.getUniqueName());
        }
        return Collections.unmodifiableSet(names);
    }

    public SortedSet<Integer> getNaturalOrderPositions() {
        return Collections.unmodifiableSortedSet(resources.getNaturalOrderPositions());
    }

    public SortedSet<Integer> getReverseOrderPositions() {
        return Collections.unmodifiableSortedSet(resources.getReverseOrderPositions());
    }

    public List<XAResourceHolderState> getNaturalOrderResourcesForPosition(Integer position) {
        return Collections.unmodifiableList(resources.getByNaturalOrderForPosition(position));
    }

    public List<XAResourceHolderState> getReverseOrderResourcesForPosition(Integer position) {
        return Collections.unmodifiableList(resources.getByReverseOrderForPosition(position));
    }

    public List<XAResourceHolderState> getAllResources() {
        List<XAResourceHolderState> result = new ArrayList<XAResourceHolderState>(resources.size());
        for (Integer positionKey : resources.getNaturalOrderPositions()) {
            result.addAll(resources.getByNaturalOrderForPosition(positionKey));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Get the enlisted resources count.
     * @return the enlisted resources count.
     */
    public int size() {
        return resources.size();
    }

    /**
     * Get the GTRID of the transaction the {@link XAResourceManager} instance is attached to.
     * @return the GTRID of the transaction the {@link XAResourceManager} instance is attached to.
     */
    public Uid getGtrid() {
        return gtrid;
    }

    /**
     * Return a human-readable representation of this object.
     * @return a human-readable representation of this object.
     */
    public String toString() {
        return "a XAResourceManager with GTRID [" + gtrid + "] and " + resources;
    }

}
