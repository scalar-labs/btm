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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of all services required by a {@link XAStatefulHolder}.
 *
 * @author Ludovic Orban
 */
public abstract class AbstractXAStatefulHolder implements XAStatefulHolder {

    private final static Logger log = LoggerFactory.getLogger(AbstractXAStatefulHolder.class);

    private volatile State state = State.IN_POOL;
    private final List<StateChangeListener> stateChangeEventListeners = new CopyOnWriteArrayList<StateChangeListener>();
    private final Date creationDate = new Date();

    @Override
    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        State oldState = this.state;
        fireStateChanging(oldState, state);

        if (oldState == state)
            throw new IllegalArgumentException("cannot switch state from " + oldState +
                    " to " + state);

        if (log.isDebugEnabled()) log.debug("state changing from " + oldState +
                " to " + state + " in " + this);

        this.state = state;

        fireStateChanged(oldState, state);
    }

    @Override
    public void addStateChangeEventListener(StateChangeListener listener) {
        stateChangeEventListeners.add(listener);
    }

    @Override
    public void removeStateChangeEventListener(StateChangeListener listener) {
        stateChangeEventListeners.remove(listener);
    }

    private void fireStateChanging(State currentState, State futureState) {
        if (log.isDebugEnabled()) log.debug("notifying " + stateChangeEventListeners.size() +
                " stateChangeEventListener(s) about state changing from " + currentState +
                " to " + futureState + " in " + this);

        for (StateChangeListener stateChangeListener : stateChangeEventListeners) {
            stateChangeListener.stateChanging(this, currentState, futureState);
        }
    }

    private void fireStateChanged(State oldState, State newState) {
        if (log.isDebugEnabled()) log.debug("notifying " + stateChangeEventListeners.size() +
                " stateChangeEventListener(s) about state changed from " + oldState +
                " to " + newState + " in " + this);

        for (StateChangeListener stateChangeListener : stateChangeEventListeners) {
            stateChangeListener.stateChanged(this, oldState, newState);
        }
    }
}
