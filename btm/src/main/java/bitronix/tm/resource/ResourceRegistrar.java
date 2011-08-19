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
package bitronix.tm.resource;

import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.recovery.IncrementalRecoverer;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.TransactionManagerServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.xa.XAResource;
import java.util.*;

/**
 * Collection of initialized {@link XAResourceProducer}s. All resources must be registered in the {@link ResourceRegistrar}
 * before they can be used by the transaction manager.
 *
 * @author lorban
 */
public class ResourceRegistrar {

    private final static Logger log = LoggerFactory.getLogger(ResourceRegistrar.class);

    private final static Map resources = new HashMap();

    /**
     * Get a registered {@link XAResourceProducer}.
     * @param uniqueName the name of the recoverable resource producer.
     * @return the {@link XAResourceProducer} or null if there was none registered under that name.
     */
    public synchronized static XAResourceProducer get(String uniqueName) {
        return (XAResourceProducer) resources.get(uniqueName);
    }

    /**
     * Get all {@link XAResourceProducer}s unique names.
     * @return a Set containing all {@link bitronix.tm.resource.common.XAResourceProducer}s unique names.
     */
    public synchronized static Set getResourcesUniqueNames() {
        return new HashSet(resources.keySet());
    }

    /**
     * Register a {@link XAResourceProducer}. If registration happens after the transaction manager started, incremental
     * recovery is run on that resource.
     * @param producer the {@link XAResourceProducer}.
     * @throws bitronix.tm.recovery.RecoveryException when an error happens during recovery.
     */
    public synchronized static void register(XAResourceProducer producer) throws RecoveryException {
        String uniqueName = producer.getUniqueName();
        if (producer.getUniqueName() == null)
            throw new IllegalArgumentException("invalid resource with null uniqueName");
        if (resources.containsKey(uniqueName))
            throw new IllegalArgumentException("resource with uniqueName '" + producer.getUniqueName() + "' has already been registered");

        if (TransactionManagerServices.isTransactionManagerRunning()) {
            if (log.isDebugEnabled()) { log.debug("transaction manager is running, recovering resource " + uniqueName); }
            IncrementalRecoverer.recover(producer);
        }

        resources.put(uniqueName, producer);
    }

    /**
     * Unregister a previously registered {@link XAResourceProducer}.
     * @param producer the {@link XAResourceProducer}.
     */
    public synchronized static void unregister(XAResourceProducer producer) {
        String uniqueName = producer.getUniqueName();
        if (producer.getUniqueName() == null)
            throw new IllegalArgumentException("invalid resource with null uniqueName");
        if (!resources.containsKey(uniqueName)) {
            if (log.isDebugEnabled()) { log.debug("resource with uniqueName '" + producer.getUniqueName() + "' has not been registered"); }
            return;
        }
        resources.remove(uniqueName);
    }

    /**
     * Find in the registered {@link XAResourceProducer}s the {@link XAResourceHolder} from which the specified {@link XAResource} comes from.
     * @param xaResource the {@link XAResource} to look for
     * @return the associated {@link XAResourceHolder} or null if it cannot be found.
     */
    public synchronized static XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        Iterator it = resources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            XAResourceProducer producer = (XAResourceProducer) entry.getValue();

            XAResourceHolder resourceHolder = producer.findXAResourceHolder(xaResource);
            if (resourceHolder != null) {
                if (log.isDebugEnabled()) { log.debug("XAResource " + xaResource + " belongs to " + resourceHolder + " that itself belongs to " + producer); }
                return resourceHolder;
            }
            if (log.isDebugEnabled()) { log.debug("XAResource " + xaResource + " does not belong to any resource of " + producer); }
        }
        return null;
    }

}
