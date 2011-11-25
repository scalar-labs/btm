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

import bitronix.tm.BitronixXid;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Uid;

import javax.transaction.xa.XAResource;
import java.util.Map;

/**
 * {@link XAResource} wrappers must implement this interface. It defines a way to get access to the transactional
 * state of this {@link XAResourceHolder}.
 *
 * @see XAResourceHolderState
 * @author lorban
 */
public interface XAResourceHolder extends XAStatefulHolder {

    /**
     * Get the vendor's {@link XAResource} implementation of the wrapped resource.
     * @return the vendor's XAResource implementation.
     */
    public XAResource getXAResource();

    /**
     * Get all the {@link XAResourceHolderState}s of this wrapped resource for a specific GTRID.
     * <p>The returned Map is guaranteed to return states in order they were added when its values are iterated.</p>
     * @param gtrid the GTRID of the transaction state to add.
     * @return the {@link XAResourceHolderState}.
     */
    public Map<Uid, XAResourceHolderState> getXAResourceHolderStatesForGtrid(Uid gtrid);

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
