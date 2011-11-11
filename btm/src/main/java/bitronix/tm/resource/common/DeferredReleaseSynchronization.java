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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Synchronization;

/**
 * {@link Synchronization} used to release a {@link XAStatefulHolder} object after 2PC has executed.
 *
 * @author lorban
 */
public class DeferredReleaseSynchronization implements Synchronization {

    private final static Logger log = LoggerFactory.getLogger(DeferredReleaseSynchronization.class);

    private final XAStatefulHolder xaStatefulHolder;

    public DeferredReleaseSynchronization(XAStatefulHolder xaStatefulHolder) {
        this.xaStatefulHolder = xaStatefulHolder;
    }

    public XAStatefulHolder getXAStatefulHolder() {
        return xaStatefulHolder;
    }

    public void afterCompletion(int status) {
        if (log.isDebugEnabled()) log.debug("DeferredReleaseSynchronization requeuing " + xaStatefulHolder);

        // set this connection's state back to IN_POOL
        xaStatefulHolder.setState(XAResourceHolder.STATE_IN_POOL);

        if (log.isDebugEnabled()) log.debug("DeferredReleaseSynchronization requeued " + xaStatefulHolder);
    }

    public void beforeCompletion() {
    }

    public String toString() {
        return "a DeferredReleaseSynchronization of " + xaStatefulHolder;
    }
}
