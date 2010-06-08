package bitronix.tm.resource.ehcache;

import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;

import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * EHCache implementation of BTM's XAResourceHolder.
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
     * Method is only there to remain compatible with pre-2.0.0 version of BTM
     * @return the ResourceBean associated with this Resource
     * @deprecated for compatibility with pre-2.0.0 version of BTM
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
    public List getXAResourceHolders() {
        List xaResourceHolders = new ArrayList(1);
        xaResourceHolders.add(this);
        return xaResourceHolders;
    }

}
