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

import java.util.*;

import org.slf4j.*;

import bitronix.tm.utils.Decoder;

/**
 * Implementation of all services required by a {@link XAStatefulHolder}.
 *
 * @author lorban
 */
public abstract class AbstractXAStatefulHolder implements XAStatefulHolder {

    private final static Logger log = LoggerFactory.getLogger(AbstractXAStatefulHolder.class);

    private volatile int state = STATE_IN_POOL;
    private final List<StateChangeListener> stateChangeEventListeners = new ArrayList<StateChangeListener>();

    public int getState() {
        return state;
    }

    public void setState(int state) {
        int oldState = this.state;
        fireStateChanging(oldState, state);

        if (oldState == state)
            throw new IllegalArgumentException("cannot switch state from " + Decoder.decodeXAStatefulHolderState(oldState) +
                    " to " + Decoder.decodeXAStatefulHolderState(state));

        if (log.isDebugEnabled()) log.debug("state changing from " + Decoder.decodeXAStatefulHolderState(oldState) +
                " to " + Decoder.decodeXAStatefulHolderState(state) + " in " + this);

        this.state = state;

        fireStateChanged(oldState, state);
    }

    public void addStateChangeEventListener(StateChangeListener listener) {
        stateChangeEventListeners.add(listener);
    }

    public void removeStateChangeEventListener(StateChangeListener listener) {
        stateChangeEventListeners.remove(listener);
    }

    private void fireStateChanging(int currentState, int futureState) {
        if (log.isDebugEnabled()) log.debug("notifying " + stateChangeEventListeners.size() +
                " stateChangeEventListener(s) about state changing from " + Decoder.decodeXAStatefulHolderState(currentState) +
                " to " + Decoder.decodeXAStatefulHolderState(futureState) + " in " + this);

        for (StateChangeListener stateChangeListener : stateChangeEventListeners) {
            stateChangeListener.stateChanging(this, currentState, futureState);
        }
    }

    private void fireStateChanged(int oldState, int newState) {
        if (log.isDebugEnabled()) log.debug("notifying " + stateChangeEventListeners.size() +
                " stateChangeEventListener(s) about state changed from " + Decoder.decodeXAStatefulHolderState(oldState) +
                " to " + Decoder.decodeXAStatefulHolderState(newState) + " in " + this);

        for (StateChangeListener stateChangeListener : stateChangeEventListeners) {
            stateChangeListener.stateChanged(this, oldState, newState);
        }
    }
}
