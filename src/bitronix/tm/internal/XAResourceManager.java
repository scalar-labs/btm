package bitronix.tm.internal;

import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.Scheduler;
import bitronix.tm.BitronixXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.util.*;

/**
 * Every {@link bitronix.tm.BitronixTransaction} contains an instance of this class that is used to register
 * and keep track of resources enlisted in a transaction.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class XAResourceManager {

    private final static Logger log = LoggerFactory.getLogger(XAResourceManager.class);

    private Uid gtrid;
    private Scheduler resources = new Scheduler();

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
        if (flag != XAResource.TMJOIN && xaResourceHolderState.getTwoPcOrderingPosition() == Scheduler.ALWAYS_LAST_POSITION) {
            List alwaysLastResources = resources.getByNaturalOrderForPosition(Scheduler.ALWAYS_LAST_POSITION_KEY);
            if (alwaysLastResources != null && alwaysLastResources.size() > 0)
                throw new BitronixSystemException("cannot enlist more than one non-XA resource, tried enlisting " + xaResourceHolderState + ", already enlisted: " + alwaysLastResources.get(0));
        }

        xaResourceHolderState.setXid(xid);
        xaResourceHolderState.start(flag);
        if (log.isDebugEnabled()) log.debug("started " + xaResourceHolderState + " with " + Decoder.decodeXAResourceFlag(flag));


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
        Iterator it = resources.iterator();
        while (it.hasNext()) {
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) it.next();
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
        List toBeReEnlisted = new ArrayList();

        Iterator it = resources.iterator();
        while (it.hasNext()) {
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) it.next();
            if (log.isDebugEnabled()) log.debug("resuming " + xaResourceHolderState);

            // The {@link XAResourceHolder} got borrowed by the new {@link XAResourceHolderState} created after suspend.
            // Its reference to the old {@link XAResourceHolderState} has changed for the new one at the time the new
            // one been enlisted. Now that we're switching back to the old context, we need to reattach the pooled
            // connection with the {@link XAResourceHolderState} that it was attached to before suspension. Fortunately,
            // the link {@link XAResourceHolderState} <-> {@link XAResourceHolder} is bi-directional so we can just loop
            // over all enlisted {@link XAResourceHolderState} and set the {@link XAResourceHolder} they contain's
            // reference back to them.
            // See: {@link bitronix.tm.resource.common.XAResourceHolder#getXAResourceHolderState()}
            xaResourceHolderState.getXAResourceHolder().setXAResourceHolderState(xaResourceHolderState);

            // If a prepared statement is (re-)used after suspend/resume is performed its XAResource needs to be
            // re-enlisted. This must be done outside this loop or that will confuse the iterator!
            toBeReEnlisted.add(new XAResourceHolderState(xaResourceHolderState));
        }

        if (toBeReEnlisted.size() > 0 && log.isDebugEnabled()) log.debug("re-enlisting " + toBeReEnlisted.size() + " resource(s)");
        for (int i = 0; i < toBeReEnlisted.size(); i++) {
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) toBeReEnlisted.get(i);

            if (log.isDebugEnabled()) log.debug("re-enlisting resource " + xaResourceHolderState);
            xaResourceHolderState.getXAResourceHolder().setXAResourceHolderState(xaResourceHolderState);
            try {
                enlist(xaResourceHolderState);
            } catch (BitronixSystemException ex) {
                throw new BitronixXAException("error re-enlisting resource during resume: " + xaResourceHolderState, XAException.XAER_PROTO, ex);
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
        Iterator it = resources.iterator();
        while (it.hasNext()) {
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) it.next();

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

        Iterator it = resources.iterator();
        while (it.hasNext()) {
            XAResourceHolderState alreadyEnlistedHolderState = (XAResourceHolderState) it.next();

            if (log.isDebugEnabled()) log.debug("checking joinability of " + xaResourceHolderState + " with " + alreadyEnlistedHolderState);
            if ( alreadyEnlistedHolderState.isEnded() &&
                 !alreadyEnlistedHolderState.isSuspended() &&
                 xaResourceHolderState.getXAResource().isSameRM(alreadyEnlistedHolderState.getXAResource()) ) {
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
        Iterator it = resources.iterator();
        while (it.hasNext()) {
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) it.next();

            // clear out the current state
            xaResourceHolderState.getXAResourceHolder().setXAResourceHolderState(null);

            // After a JOIN happened, the same xaResourceHolderState is inserted twice.
            // This is because the xaResourceHolderState is inserted once normally during the 1st half of the transaction
            // and right after the transaction is resumed, another xaResourceHolderState with a null XID is inserted
            // then the XID is changed.
            boolean mightHaveMore = true;
            while (mightHaveMore) {
                mightHaveMore = xaResourceHolderState.getXAResourceHolder().removeXAResourceHolderState(xaResourceHolderState);
                if (mightHaveMore && log.isDebugEnabled()) log.debug("cleared state on " + xaResourceHolderState);
            }

            it.remove();
        }
    }

    /**
     * Get a {@link Set} of unique names of all the enlisted {@link XAResourceHolderState}s.
     * @return a {@link Set} of unique names of all the enlisted {@link XAResourceHolderState}s.
     */
    public Set collectUniqueNames() {
        Set names = new HashSet();
        Iterator it = resources.iterator();
        while (it.hasNext()) {
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) it.next();
            names.add(xaResourceHolderState.getUniqueName());
        }
        return names;
    }

    public SortedSet getNaturalOrderPositions() {
        return resources.getNaturalOrderPositions();
    }

    public SortedSet getReverseOrderPositions() {
        return resources.getReverseOrderPositions();
    }

    public List getNaturalOrderResourcesForPosition(Object positionKey) {
        return resources.getByNaturalOrderForPosition(positionKey);
    }

    public List getReverseOrderResourcesForPosition(Object positionKey) {
        return resources.getByReverseOrderForPosition(positionKey);
    }

    public List getAllResources() {
        List result = new ArrayList();
        Iterator it = resources.getNaturalOrderPositions().iterator();
        while (it.hasNext()) {
            Object positionKey = it.next();
            result.addAll(resources.getByNaturalOrderForPosition(positionKey));
        }
        return result;
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
