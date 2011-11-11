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
