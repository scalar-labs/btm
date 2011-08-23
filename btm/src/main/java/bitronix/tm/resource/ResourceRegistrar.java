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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.recovery.IncrementalRecoverer;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAResourceProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.xa.XAResource;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Collection of initialized {@link XAResourceProducer}s. All resources must be registered in the {@link ResourceRegistrar}
 * before they can be used by the transaction manager.
 * <p/>
 * Note: The implementation is based on a thread safe, read-optimized list (copy-on-write) assuming that the
 * number of registered resources is around 1 to 16 entries and does not change often. If this assumption is
 * not-true it may be required to re-implement this with a ConcurrentMap instead.
 *
 * @author lorban, jkellerer
 */
public class ResourceRegistrar {

    private final static Logger log = LoggerFactory.getLogger(ResourceRegistrar.class);

    private final static CopyOnWriteArraySet<ProducerHolder> resources = new CopyOnWriteArraySet<ProducerHolder>();

    /**
     * Get a registered {@link XAResourceProducer}.
     *
     * @param uniqueName the name of the recoverable resource producer.
     * @return the {@link XAResourceProducer} or null if there was none registered under that name.
     */
    public static XAResourceProducer get(final String uniqueName) {
        if (uniqueName != null) {
            for (ProducerHolder holder : resources)
                if (uniqueName.equals(holder.getUniqueName()))
                    return holder.producer;
        }
        return null;
    }

    /**
     * Get all {@link XAResourceProducer}s unique names.
     *
     * @return a Set containing all {@link bitronix.tm.resource.common.XAResourceProducer}s unique names.
     */
    public static Set<String> getResourcesUniqueNames() {
        final Set<String> names = new HashSet<String>(resources.size());
        for (ProducerHolder holder : resources)
            names.add(holder.getUniqueName());

        return names;
    }

    /**
     * Register a {@link XAResourceProducer}. If registration happens after the transaction manager started, incremental
     * recovery is run on that resource.
     * <p/>
     * Note: We need to synchronized here as we cannot combine ".contains(...), .recover(...) and .add(...)",
     *       as long as recovery cannot run on already added resources.
     *
     * @param producer the {@link XAResourceProducer}.
     * @throws bitronix.tm.recovery.RecoveryException When an error happens during recovery.
     */
    public static synchronized void register(XAResourceProducer producer) throws RecoveryException {
        final ProducerHolder holder = new ProducerHolder(producer);

        if (resources.contains(holder)) {
            throw new IllegalArgumentException("resource with uniqueName '" +
                    holder.getUniqueName() + "' has already been registered");
        }

        if (TransactionManagerServices.isTransactionManagerRunning()) {
            if (log.isDebugEnabled()) { log.debug("transaction manager is running, recovering resource {}.", holder.getUniqueName()); }
            IncrementalRecoverer.recover(producer);
        }

        if (!resources.add(holder)) {
            log.error("Failed adding resource producer holder {}. This should never happen.", holder);
        }
    }

    /**
     * Unregister a previously registered {@link XAResourceProducer}.
     *
     * @param producer the {@link XAResourceProducer}.
     */
    public static void unregister(XAResourceProducer producer) {
        final ProducerHolder holder = new ProducerHolder(producer);

        if (!resources.remove(holder)) {
            if (log.isDebugEnabled()) { log.debug("resource with uniqueName '{}' has not been registered", holder.getUniqueName()); }
        }
    }

    /**
     * Find in the registered {@link XAResourceProducer}s the {@link XAResourceHolder} from which the specified {@link XAResource} comes from.
     *
     * @param xaResource the {@link XAResource} to look for
     * @return the associated {@link XAResourceHolder} or null if it cannot be found.
     */
    public static XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        final boolean debug = log.isDebugEnabled();

        for (ProducerHolder resource : resources) {
            final XAResourceProducer producer = resource.producer;
            final XAResourceHolder resourceHolder = producer.findXAResourceHolder(xaResource);
            if (resourceHolder != null) {
                if (debug) { log.debug("XAResource " + xaResource + " belongs to " + resourceHolder + " that itself belongs to " + producer); }
                return resourceHolder;
            }
            if (debug) { log.debug("XAResource " + xaResource + " does not belong to any resource of " + producer); }
        }

        return null;
    }

    public final static class ProducerHolder implements Serializable {

        private XAResourceProducer producer;

        public ProducerHolder() {
        }

        private ProducerHolder(XAResourceProducer producer) {
            if (producer == null)
                throw new IllegalArgumentException("invalid null resource");
            if (producer.getUniqueName() == null)
                throw new IllegalArgumentException("invalid resource with null uniqueName");

            this.producer = producer;
        }

        public String getUniqueName() {
            return producer.getUniqueName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProducerHolder)) return false;
            ProducerHolder that = (ProducerHolder) o;
            return getUniqueName().equals(that.getUniqueName());
        }

        @Override
        public int hashCode() {
            return getUniqueName().hashCode();
        }

        @Override
        public String toString() {
            return "ProducerHolder{" +
                    "producer=" + producer +
                    '}';
        }
    }
}
