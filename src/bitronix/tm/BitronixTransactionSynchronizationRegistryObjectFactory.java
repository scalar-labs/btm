package bitronix.tm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * {@link bitronix.tm.BitronixTransactionSynchronizationRegistry} object factory for JNDI references.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception {
        if (log.isDebugEnabled()) log.debug("returning the unique synchronization registry instance");
        return TransactionManagerServices.getTransactionSynchronizationRegistry();
    }


}
