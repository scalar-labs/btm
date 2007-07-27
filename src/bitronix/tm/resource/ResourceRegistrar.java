package bitronix.tm.resource;

import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAResourceProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.xa.XAResource;
import java.util.*;

/**
 * Collection of initialized {@link XAResourceProducer}s. All resources must be registered in the {@link ResourceRegistrar}
 * before they can be used by the transaction manager.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class ResourceRegistrar {

    private final static Logger log = LoggerFactory.getLogger(ResourceRegistrar.class);

    private static Map resources = new HashMap();

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
     * Register a {@link XAResourceProducer}.
     * @param producer the {@link XAResourceProducer}.
     */
    public synchronized static void register(XAResourceProducer producer) {
        String uniqueName = producer.getUniqueName();
        if (producer.getUniqueName() == null)
            throw new IllegalArgumentException("invalid resource with null uniqueName");
        if (resources.containsKey(uniqueName))
            throw new IllegalArgumentException("resource with uniqueName '" + producer.getUniqueName() + "' has already been registered");
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
        if (!resources.containsKey(uniqueName))
            throw new IllegalArgumentException("resource with uniqueName '" + producer.getUniqueName() + "' has not been registered");
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

            XAResourceHolder XAResourceHolder = producer.findXAResourceHolder(xaResource);
            if (XAResourceHolder != null) {
                if (log.isDebugEnabled()) log.debug("XAResource " + xaResource + " belongs to " + XAResourceHolder + " that itself belongs to " + producer);
                return XAResourceHolder;
            }
            if (log.isDebugEnabled()) log.debug("XAResource " + xaResource + " does not belong to any resource of " + producer);
        }
        return null;
    }

}
