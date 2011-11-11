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
package bitronix.tm.resource.ehcache;

import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;

import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Ehcache implementation of BTM's XAResourceHolder.
 * <p>
 *   Copyright 2003-2010 Terracotta, Inc.
 * </p>
 * @author lorban
 */
public class EhCacheXAResourceHolder extends AbstractXAResourceHolder {

    private final XAResource resource;
    private final ResourceBean bean;

    /**
     * Create a new EhCacheXAResourceHolder for a particular XAResource
     * @param resource the required XAResource
     * @param bean the required ResourceBean
     */
    public EhCacheXAResourceHolder(XAResource resource, ResourceBean bean) {
        this.resource = resource;
        this.bean = bean;
    }

    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() {
        return resource;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceBean getResourceBean() {
        return bean;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws Exception {
        throw new UnsupportedOperationException("EhCacheXAResourceHolder cannot be used with an XAPool");
    }

    /**
     * {@inheritDoc}
     */
    public Object getConnectionHandle() throws Exception {
        throw new UnsupportedOperationException("EhCacheXAResourceHolder cannot be used with an XAPool");
    }

    /**
     * {@inheritDoc}
     */
    public Date getLastReleaseDate() {
        throw new UnsupportedOperationException("EhCacheXAResourceHolder cannot be used with an XAPool");
    }

    /**
     * {@inheritDoc}
     */
    public List<XAResourceHolder> getXAResourceHolders() {
        return Arrays.asList((XAResourceHolder) this);
    }

}
