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
package bitronix.tm.resource.ehcache;

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EHCache implementation of BTM's XAResourceProducer.
 * <p>
 *   Copyright 2003-2010 Terracotta, Inc.
 * </p>
 * @author Ludovic Orban
 */
@SuppressWarnings("serial")
public final class EhCacheXAResourceProducer extends ResourceBean implements XAResourceProducer<EhCacheXAResourceHolder, EhCacheXAResourceHolder> {

    private static final Logger log = LoggerFactory.getLogger(EhCacheXAResourceProducer.class.getName());

    private static final ConcurrentMap<String, EhCacheXAResourceProducer> producers = new ConcurrentHashMap<String, EhCacheXAResourceProducer>();

    private final ConcurrentMap<Integer, EhCacheXAResourceHolder> xaResourceHolders = new ConcurrentHashMap<Integer, EhCacheXAResourceHolder>();
    private final AtomicInteger xaResourceHolderCounter = new AtomicInteger();
    private volatile RecoveryXAResourceHolder recoveryXAResourceHolder;


    private EhCacheXAResourceProducer() {
        setApplyTransactionTimeout(true);
    }


    /**
     * Register an XAResource of a cache with BTM. The first time a XAResource is registered a new
     * EhCacheXAResourceProducer is created to hold it.
     * @param uniqueName the uniqueName of this XAResourceProducer, usually the cache's name
     * @param xaResource the XAResource to be registered
     */
    public static void registerXAResource(String uniqueName, XAResource xaResource) {
        EhCacheXAResourceProducer xaResourceProducer = producers.get(uniqueName);
        if (xaResourceProducer == null) {
            xaResourceProducer = new EhCacheXAResourceProducer();
            xaResourceProducer.setUniqueName(uniqueName);
            // the initial xaResource must be added before init() can be called
            xaResourceProducer.addXAResource(xaResource);

            EhCacheXAResourceProducer previous = producers.putIfAbsent(uniqueName, xaResourceProducer);
            if (previous == null) {
                xaResourceProducer.init();
            } else {
                previous.addXAResource(xaResource);
            }
        } else {
            xaResourceProducer.addXAResource(xaResource);
        }
    }

    /**
     * Unregister an XAResource of a cache from BTM.
     * @param uniqueName the uniqueName of this XAResourceProducer, usually the cache's name
     * @param xaResource the XAResource to be registered
     */
    public static void unregisterXAResource(String uniqueName, XAResource xaResource) {
        EhCacheXAResourceProducer xaResourceProducer = producers.get(uniqueName);

        if (xaResourceProducer != null) {
            boolean found = xaResourceProducer.removeXAResource(xaResource);
            if (!found) {
                log.error("no XAResource " + xaResource + " found in XAResourceProducer with name " + uniqueName);
            }
            if (xaResourceProducer.xaResourceHolders.isEmpty()) {
                xaResourceProducer.close();
                producers.remove(uniqueName);
            }
        } else {
            log.error("no XAResourceProducer registered with name " + uniqueName);
        }
    }


    private void addXAResource(XAResource xaResource) {
        EhCacheXAResourceHolder xaResourceHolder = new EhCacheXAResourceHolder(xaResource, this);
        int key = xaResourceHolderCounter.incrementAndGet();

        xaResourceHolders.put(key, xaResourceHolder);
    }

    private boolean removeXAResource(XAResource xaResource) {
        for (Map.Entry<Integer, EhCacheXAResourceHolder> entry : xaResourceHolders.entrySet()) {
            Integer key = entry.getKey();
            EhCacheXAResourceHolder xaResourceHolder = entry.getValue();
            if (xaResourceHolder.getXAResource() == xaResource) {
                xaResourceHolders.remove(key);
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XAResourceHolderState startRecovery() throws RecoveryException {
        if (recoveryXAResourceHolder != null) {
            throw new RecoveryException("recovery already in progress on " + this);
        }

        if (xaResourceHolders.isEmpty()) {
            throw new RecoveryException("no XAResource registered, recovery cannot be done on " + this);
        }

        recoveryXAResourceHolder = new RecoveryXAResourceHolder(xaResourceHolders.values().iterator().next());
        return new XAResourceHolderState(recoveryXAResourceHolder, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endRecovery() throws RecoveryException {
        recoveryXAResourceHolder = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFailed(boolean failed) {
        // cache cannot fail as it's not connection oriented
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EhCacheXAResourceHolder findXAResourceHolder(XAResource xaResource) {
        for (EhCacheXAResourceHolder xaResourceHolder : xaResourceHolders.values()) {
            if (xaResource == xaResourceHolder.getXAResource()) {
                return xaResourceHolder;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        try {
            ResourceRegistrar.register(this);
        } catch (RecoveryException ex) {
            throw new BitronixRuntimeException("error recovering " + this, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        xaResourceHolders.clear();
        xaResourceHolderCounter.set(0);
        ResourceRegistrar.unregister(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EhCacheXAResourceHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
        throw new UnsupportedOperationException("Ehcache is not connection-oriented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reference getReference() throws NamingException {
        return new Reference(EhCacheXAResourceProducer.class.getName(),
                new StringRefAddr("uniqueName", getUniqueName()),
                ResourceObjectFactory.class.getName(), null);
    }

    @Override
    public String toString() {
        return "a EhCacheXAResourceProducer with uniqueName " + getUniqueName();
    }
}
