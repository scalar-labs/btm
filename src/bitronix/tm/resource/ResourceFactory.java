package bitronix.tm.resource;

import bitronix.tm.TransactionManagerServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * {@link bitronix.tm.resource.common.XAResourceProducer} object factory for JNDI references.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @see bitronix.tm.resource.common.ResourceBean
 * @author lorban
 */
public class ResourceFactory implements ObjectFactory {

    private final static Logger log = LoggerFactory.getLogger(ResourceFactory.class);

    public Object getObjectInstance(Object obj, Name jndiNameObject, Context nameCtx, Hashtable environment) throws Exception {
        // first make sure the TM started so resources get a chance to be configured
        TransactionManagerServices.getTransactionManager();

        Reference ref = (Reference) obj;
        if (log.isDebugEnabled()) log.debug("referencing resource with reference of type " + ref.getClass());

        RefAddr refAddr = ref.get("uniqueName");
        if (refAddr == null)
            throw new NamingException("no 'uniqueName' RefAddr found");
        String uniqueName = (String) refAddr.getContent();

        if (log.isDebugEnabled()) log.debug("getting registered resource with uniqueName '" + uniqueName + "'");
        Referenceable resource = ResourceRegistrar.get(uniqueName);
        if (resource == null)
            throw new NamingException("no resource registered with uniqueName '" + uniqueName + "', available resources: " + availableResourcesAsString());

        return resource;
    }

    private String availableResourcesAsString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");

        Iterator it = ResourceRegistrar.getResourcesUniqueNames().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            sb.append(name);
            if (it.hasNext())
                sb.append(", ");
        }

        sb.append("]");
        return sb.toString();
    }

}
