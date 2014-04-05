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
package bitronix.tm.resource.common;

import bitronix.tm.BitronixXid;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Uid;

import javax.transaction.xa.XAResource;

/**
 * {@link XAResource} wrappers must implement this interface. It defines a way to get access to the transactional
 * state of this {@link XAResourceHolder}.
 *
 * @see XAResourceHolderState
 * @author Ludovic Orban
 */
public interface XAResourceHolder<T extends XAStatefulHolder> extends XAStatefulHolder<T> {

    /**
     * Get the vendor's {@link XAResource} implementation of the wrapped resource.
     * @return the vendor's XAResource implementation.
     */
    public XAResource getXAResource();

    /**
     * This method implements a standard Visitor Pattern.  For the specified GTRID, the
     * provided {@link XAResourceHolderStateVisitor}'s visit() method is called for each matching
     * {@link XAResourceHolderState} in the order they were added.  This method was introduced
     * as a replacement for the old getXAResourceHolderStatesForGtrid(Uid) method.  The old
     * getXAResourceHolderStatesForGtrid method exported an internal collection which was unsynchronized
     * yet was iterated over by the callers.  Using the Visitor Pattern allows us to perform the same
     * iteration within the context of a lock, and avoids exposing internal state and implementation
     * details to callers.
     * @param gtrid the GTRID of the transaction state to visit {@link XAResourceHolderState}s for
     * @param visitor a {@link XAResourceHolderStateVisitor} instance
     */
    public void acceptVisitorForXAResourceHolderStates(Uid gtrid, XAResourceHolderStateVisitor visitor);

    /**
     * Checks whether there are {@link XAResourceHolderState}s for the specified GTRID.
     * @param gtrid the GTRID of the transaction state to check existence for
     * @return true if there are {@link XAResourceHolderState}s, false otherwise
     */
    public boolean isExistXAResourceHolderStatesForGtrid(Uid gtrid);

    /**
     * Get a count of {@link XAResourceHolderState}s for the specified GTRID.
     * @param gtrid the GTRID to get a {@link XAResourceHolderState} count for
     * @return the count of {@link XAResourceHolderState}s, or 0 if there are no states for the
     * specified GTRID
     */
    public int getXAResourceHolderStateCountForGtrid(Uid gtrid);

    /**
     * Add a {@link XAResourceHolderState} of this wrapped resource.
     * @param xid the Xid of the transaction state to add.
     * @param xaResourceHolderState the {@link XAResourceHolderState} to set.
     */
    public void putXAResourceHolderState(BitronixXid xid, XAResourceHolderState xaResourceHolderState);


    /**
     * Remove all states related to a specific Xid from this wrapped resource.
     * @param xid the Xid of the transaction state to remove.
     */
    public void removeXAResourceHolderState(BitronixXid xid);

    /**
     * Check if this {@link XAResourceHolder} contains a state for a specific {@link XAResourceHolder}.
     * In other words: has the {@link XAResourceHolder}'s {@link XAResource} been enlisted in some transaction ?
     * @param xaResourceHolder the {@link XAResourceHolder} to look for.
     * @return true if the {@link XAResourceHolder} is enlisted in some transaction, false otherwise.
     */
    public boolean hasStateForXAResource(XAResourceHolder xaResourceHolder);

    /**
     * Get the ResourceBean which created this XAResourceHolder.
     * @return the ResourceBean which created this XAResourceHolder.
     */
    public ResourceBean getResourceBean();

}
