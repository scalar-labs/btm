package bitronix.tm.internal;

import bitronix.tm.BitronixTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.*;

/**
 * Every {@link bitronix.tm.BitronixTransaction} contains an instance of this class that is used to register
 * and keep track of resources enlisted in a transaction.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class XAResourceManager {

    private final static Logger log = LoggerFactory.getLogger(XAResourceManager.class);

    private Uid gtrid;
    private Map resources = new HashMap();

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
     * @throws XAException
     * @throws BitronixSystemException
     */
    public void enlist(XAResourceHolderState xaResourceHolderState) throws XAException, BitronixSystemException {
        XAResourceHolderState emulatingXAResourceHolderState = findEmulatingXAResourceHolderState();
        if (xaResourceHolderState.getXAResourceHolder().isEmulatingXA() && emulatingXAResourceHolderState != null)
            throw new BitronixSystemException("cannot enlist more than one non-XA resource, tried enlisting " + xaResourceHolderState + ", already enlisted: " + emulatingXAResourceHolderState);

        XAResourceHolderState alreadyEnlistedHolder = findXAResourceHolderState(xaResourceHolderState.getXAResource());
        if (alreadyEnlistedHolder != null && !alreadyEnlistedHolder.isEnded()) {
            xaResourceHolderState.setXid(alreadyEnlistedHolder.getXid());
            log.warn("ignoring enlistment of already enlisted but not ended resource " + xaResourceHolderState);
            return;
        }

        XAResourceHolderState toBeJoinedHolderState = null;
        if (alreadyEnlistedHolder != null) {
            if (log.isDebugEnabled()) log.debug("resource already enlisted but has been ended: " + alreadyEnlistedHolder);
            toBeJoinedHolderState = getManagedResourceWithSameRM(xaResourceHolderState);
        }

        Xid xid;
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

        xaResourceHolderState.setXid(xid);
        xaResourceHolderState.start(flag);

        // this has to be done only after start() successfully returned
        resources.put(xid, xaResourceHolderState);
    }

    /**
     * Delist the specified {@link XAResourceHolderState}. A reference to the resource is kept anyway.
     * @param xaResourceHolderState the {@link XAResourceHolderState} to be delisted.
     * @param flag the delistment flag.
     * @return true if the resource could be delisted, false otherwise.
     * @throws XAException
     * @throws BitronixSystemException
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
     * Call end on all {@link XAResourceHolderState}s enlisted in the current transaction that haven't been ended yet.
     * This method should be called by the transaction manager when commit() or rollback() is called, prior to
     * run the two-phase commit protocol to ensure all non-closed connections that participated in the transaction
     * are ended.
     * @param transaction the transaction from which unclosed resources should be delisted.
     * @throws SystemException
     */
    public void delistUnclosedResources(BitronixTransaction transaction) {
        Iterator it = resources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) entry.getValue();
            if (!xaResourceHolderState.isEnded()) {
                if (log.isDebugEnabled()) log.debug("found unclosed resource to delist: " + xaResourceHolderState);
                try {
                    transaction.delistResource(xaResourceHolderState.getXAResource(), XAResource.TMSUCCESS);
                } catch (SystemException ex) {
                    log.warn("error delisting resource: " + xaResourceHolderState, ex);
                }
            }
        } // while
    }

    /**
     * Suspend all enlisted resources.
     * @throws XAException
     */
    public void suspend() throws XAException {
        Iterator it = resources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) entry.getValue();
            if (!xaResourceHolderState.isEnded()) {
                if (log.isDebugEnabled()) log.debug("suspending " + xaResourceHolderState);
                xaResourceHolderState.end(XAResource.TMSUCCESS);
            }
        } // while
    }

    /**
     * Resume all enlisted resources.
     *
     * @throws XAException
     */
    public void resume() throws XAException {
        Iterator it = resources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) entry.getValue();
            if (log.isDebugEnabled()) log.debug("resuming " + xaResourceHolderState);

            // The {@link XAResourceHolder} got borrowed by the new {@link XAResourceHolderState} created after suspend.
            // Its reference to the old {@link XAResourceHolderState} has changed for the new one at the time the new
            // one been enlisted. Now that we're switching back to the old context, we need to reattach the pooled
            // connection with the {@link XAResourceHolderState} that it was attached to before suspension. Fortunately,
            // the link {@link XAResourceHolderState} <-> {@link XAResourceHolder} is bi-directional so we can just loop
            // over all enlisted {@link XAResourceHolderState} and set the {@link XAResourceHolder} they contain's
            // reference back to them.
            // See: bitronix.tm.resource.common.XAResourceHolder#getXAResourceHolderState()
            xaResourceHolderState.getXAResourceHolder().setXAResourceHolderState(xaResourceHolderState);
        }
    }

    /**
     * Look if an {@link XAResource} has already been enlisted.
     * @param xaResource the {@link XAResource} to look for.
     * @return the {@link XAResourceHolderState} of the enlisted resource or null if the {@link XAResource} has not
     *         been enlisted in this {@link XAResourceManager}.
     * @throws BitronixSystemException
     */
    public XAResourceHolderState findXAResourceHolderState(XAResource xaResource) throws BitronixSystemException {
        if (xaResource instanceof XAResourceHolderState) {
            throw new BitronixSystemException("cannot find a wrapped resource using another wrapped resource, please report this");
        }

        Iterator it = resources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) entry.getValue();

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

        for (Iterator iterator = resources.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            XAResourceHolderState alreadyEnlistedHolderState = (XAResourceHolderState) entry.getValue();

            if (log.isDebugEnabled()) log.debug("checking joinability of " + alreadyEnlistedHolderState + " with " + alreadyEnlistedHolderState);
            if (alreadyEnlistedHolderState.getXAResource().isSameRM(alreadyEnlistedHolderState.getXAResource()) && alreadyEnlistedHolderState.isEnded()) {
                if (log.isDebugEnabled()) log.debug("resources are joinable");
                return alreadyEnlistedHolderState;
            }
            if (log.isDebugEnabled()) log.debug("resources are not joinable");
        }
        
        if (log.isDebugEnabled()) log.debug("no joinable resource found for " + xaResourceHolderState);
        return null;
    }

    /**
     * Find the non-XA {@link XAResourceHolderState}.
     * @return the non-XA {@link XAResourceHolderState} if one has been enlisted, null otherwise.
     */
    public XAResourceHolderState findEmulatingXAResourceHolderState() {
        Iterator it = resources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            XAResourceHolderState holder = (XAResourceHolderState) entry.getValue();
            if (holder.getXAResourceHolder().isEmulatingXA())
                return holder;
        }
        return null;
    }

    /**
     * Get a List of unique names of all the enlisted {@link XAResourceHolderState}s.
     * @return a List of unique names of all the enlisted {@link XAResourceHolderState}s.
     */
    public Set collectUniqueNames() {
        Set names = new HashSet();
        Iterator it = resources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            XAResourceHolderState xaResourceHolderState = (XAResourceHolderState) entry.getValue();
            names.add(xaResourceHolderState.getUniqueName());
        }
        return names;
    }

    /**
     * Get an Iterator on the {@link Xid}/{@link XAResourceHolderState} {@link java.util.Map.Entry} pairs registered in
     * this instance.
     * @return an Iterator on the {@link Xid}/{@link XAResourceHolderState} {@link java.util.Map.Entry} pairs.
     */
    public Iterator entriesIterator() {
        return resources.entrySet().iterator();
    }

    /**
     * Get the enlisted resources count.
     * @return the enlisted resources count.
     */
    public int size() {
        return resources.size();
    }

    /**
     * @return the GTRID of the transaction the XAResourceManager instance is attached to.
     */
    public Uid getGtrid() {
        return gtrid;
    }

    /**
     * Return a String representation of this object.
     * @return a String representation of this object.
     */
    public String toString() {
        return "a XAResourceManager with GTRID [" + gtrid + "] and " + resources.size() + " enlisted resource(s)";
    }

}
