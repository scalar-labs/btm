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

import java.util.Date;
import java.util.List;

/**
 * Dummy XAStatefulHolder class, to get the generics right.
 * @author Chris Rankin
 */
public class DummyStatefulHolder implements XAStatefulHolder<DummyStatefulHolder> {

    @Override
    public State getState() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setState(State state) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addStateChangeEventListener(StateChangeListener<DummyStatefulHolder> listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeStateChangeEventListener(StateChangeListener<DummyStatefulHolder> listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<? extends XAResourceHolder<? extends XAResourceHolder>> getXAResourceHolders() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getConnectionHandle() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Date getLastReleaseDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Date getCreationDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
