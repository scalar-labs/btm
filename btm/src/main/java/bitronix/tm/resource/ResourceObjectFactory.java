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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.spi.ObjectFactory;
import javax.naming.*;
import java.util.Hashtable;

/**
 * {@link bitronix.tm.resource.common.XAResourceProducer} object factory for JNDI references.
 *
 * @see bitronix.tm.resource.common.ResourceBean
 * @author lorban
 */
public class ResourceObjectFactory implements ObjectFactory {

    private final static Logger log = LoggerFactory.getLogger(ResourceObjectFactory.class);

    public Object getObjectInstance(Object obj, Name jndiNameObject, Context nameCtx, Hashtable<?,?> environment) throws Exception {
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
            throw new NamingException("no resource registered with uniqueName '" + uniqueName + "', available resources: " + ResourceRegistrar.getResourcesUniqueNames());

        return resource;
    }

}
