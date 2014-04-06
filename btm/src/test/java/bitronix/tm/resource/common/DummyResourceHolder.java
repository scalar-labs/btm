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
import java.util.Date;
import java.util.List;
import javax.transaction.xa.XAResource;

/**
 * Dummy XASResourceHolder class, to get the generics right.
 * @author Chris Rankin
 */
public class DummyResourceHolder implements XAResourceHolder<DummyResourceHolder> {

    @Override
    public XAResource getXAResource() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void acceptVisitorForXAResourceHolderStates(Uid gtrid, XAResourceHolderStateVisitor visitor) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isExistXAResourceHolderStatesForGtrid(Uid gtrid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getXAResourceHolderStateCountForGtrid(Uid gtrid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putXAResourceHolderState(BitronixXid xid, XAResourceHolderState xaResourceHolderState) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeXAResourceHolderState(BitronixXid xid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasStateForXAResource(XAResourceHolder<? extends XAStatefulHolder> xaResourceHolder) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResourceBean getResourceBean() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public State getState() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setState(State state) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addStateChangeEventListener(StateChangeListener<DummyResourceHolder> listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeStateChangeEventListener(StateChangeListener<DummyResourceHolder> listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<? extends XAResourceHolder<? extends XAStatefulHolder>> getXAResourceHolders() {
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
