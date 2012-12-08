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
package bitronix.tm.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * {@link bitronix.tm.resource.common.XAResourceProducer} object factory for JNDI references.
 *
 * @see bitronix.tm.resource.common.ResourceBean
 * @author Ludovic Orban
 */
public class ResourceObjectFactory implements ObjectFactory {

    private final static Logger log = LoggerFactory.getLogger(ResourceObjectFactory.class);

    public Object getObjectInstance(Object obj, Name jndiNameObject, Context nameCtx, Hashtable<?,?> environment) throws Exception {
        Reference ref = (Reference) obj;
        if (log.isDebugEnabled()) { log.debug("referencing resource with reference of type " + ref.getClass()); }

        RefAddr refAddr = ref.get("uniqueName");
        if (refAddr == null)
            throw new NamingException("no 'uniqueName' RefAddr found");
        Object content = refAddr.getContent();
        if (!(content instanceof String))
            throw new NamingException("'uniqueName' RefAddr content is not of type java.lang.String");
        String uniqueName = (String) content;

        if (log.isDebugEnabled()) { log.debug("getting registered resource with uniqueName '" + uniqueName + "'"); }
        Referenceable resource = ResourceRegistrar.get(uniqueName);
        if (resource == null)
            throw new NamingException("no resource registered with uniqueName '" + uniqueName + "', available resources: " + ResourceRegistrar.getResourcesUniqueNames());

        return resource;
    }

}
