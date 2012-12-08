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

import bitronix.tm.utils.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of all services required by a {@link XAStatefulHolder}.
 *
 * @author lorban
 */
public abstract class AbstractXAStatefulHolder implements XAStatefulHolder {

    private final static Logger log = LoggerFactory.getLogger(AbstractXAStatefulHolder.class);

    private volatile int state = STATE_IN_POOL;
    private final List<StateChangeListener> stateChangeEventListeners = new CopyOnWriteArrayList<StateChangeListener>();
    private final Date creationDate = new Date();

    public Date getCreationDate() {
        return creationDate;
    }

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
