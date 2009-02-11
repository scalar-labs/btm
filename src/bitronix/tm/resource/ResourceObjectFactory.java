package bitronix.tm.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.spi.ObjectFactory;
import javax.naming.*;
import java.util.Hashtable;

import bitronix.tm.utils.Decoder;

/**
 * {@link bitronix.tm.resource.common.XAResourceProducer} object factory for JNDI references.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @see bitronix.tm.resource.common.ResourceBean
 * @author lorban
 */
public class ResourceObjectFactory implements ObjectFactory {

    private final static Logger log = LoggerFactory.getLogger(ResourceObjectFactory.class);

    public Object getObjectInstance(Object obj, Name jndiNameObject, Context nameCtx, Hashtable environment) throws Exception {
        Reference ref = (Reference) obj;
        if (log.isDebugEnabled()) log.debug("referencing resource with reference of type " + ref.getClass());

        RefAddr refAddr = ref.get("uniqueName");
        if (refAddr == null)
            throw new NamingException("no 'uniqueName' RefAddr found");
        Object content = refAddr.getContent();
        if (!(content instanceof String))
            throw new NamingException("'uniqueName' RefAddr content is not of type java.lang.String");
        String uniqueName = (String) content;

        if (log.isDebugEnabled()) log.debug("getting registered resource with uniqueName '" + uniqueName + "'");
        Referenceable resource = ResourceRegistrar.get(uniqueName);
        if (resource == null)
            throw new NamingException("no resource registered with uniqueName '" + uniqueName + "', available resources: " + Decoder.collectResourcesNames(ResourceRegistrar.getResourcesUniqueNames()));

        return resource;
    }

}
