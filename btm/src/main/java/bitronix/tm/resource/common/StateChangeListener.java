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

import bitronix.tm.resource.common.XAStatefulHolder.State;

/**
 * {@link XAStatefulHolder} state change listener interface.
 *
 * @author Ludovic Orban
 */
public interface StateChangeListener<T extends XAStatefulHolder> {

    /**
     * Fired when the internal state of a {@link XAStatefulHolder} has changed.
     * @param source the {@link XAStatefulHolder} changing state.
     * @param oldState the old state of the {@link XAStatefulHolder}.
     * @param newState the new state of the {@link XAStatefulHolder}.
     */
    public void stateChanged(T source, State oldState, State newState);

    /**
     * Fired before the internal state of a {@link XAStatefulHolder} has changed.
     * @param source the {@link XAStatefulHolder} changing state.
     * @param currentState the current state of the {@link XAStatefulHolder}.
     * @param futureState the future state of the {@link XAStatefulHolder}.
     */
    public void stateChanging(T source, State currentState, State futureState);

}
