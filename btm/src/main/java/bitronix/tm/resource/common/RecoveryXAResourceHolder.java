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

import javax.transaction.xa.XAResource;
import java.util.Date;
import java.util.List;

/**
 * {@link XAResourceHolder} created by an {@link bitronix.tm.resource.common.XAResourceProducer} that is
 * used to perform recovery. Objects of this class cannot be used outside recovery scope.
 *
 * @author lorban
 */
public class RecoveryXAResourceHolder extends AbstractXAResourceHolder {

    private final XAResourceHolder xaResourceHolder;

    public RecoveryXAResourceHolder(XAResourceHolder xaResourceHolder) {
        this.xaResourceHolder = xaResourceHolder;
    }

    public void close() throws Exception {
        xaResourceHolder.setState(STATE_IN_POOL);
    }

    public Date getLastReleaseDate() {
        return null;
    }

    public XAResource getXAResource() {
        return xaResourceHolder.getXAResource();
    }

    public ResourceBean getResourceBean() {
        return null;
    }

    public List<XAResourceHolder> getXAResourceHolders() {
        return null;
    }

    public Object getConnectionHandle() throws Exception {
        throw new UnsupportedOperationException("illegal connection creation attempt out of " + this);
    }
}
