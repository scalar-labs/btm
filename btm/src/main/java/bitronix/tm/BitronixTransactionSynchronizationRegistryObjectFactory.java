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
package bitronix.tm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * {@link bitronix.tm.BitronixTransactionSynchronizationRegistry} object factory for JNDI references.
 *
 * @author lorban
 */
public class BitronixTransactionSynchronizationRegistryObjectFactory implements ObjectFactory {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransactionSynchronizationRegistryObjectFactory.class);

    /**
     * Since there can be only one synchronization registry per VM instance, this method always returns a reference
     * to the unique BitronixTransactionSynchronizationRegistry object.
     * @see bitronix.tm.BitronixTransactionSynchronizationRegistry
     * @return the unique synchronization registry instance.
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?,?> environment) throws Exception {
        if (log.isDebugEnabled()) log.debug("returning the unique synchronization registry instance");
        return TransactionManagerServices.getTransactionSynchronizationRegistry();
    }


}
