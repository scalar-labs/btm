package bitronix.tm;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * {@link BitronixTransactionManager} factory for JNDI references.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 * @deprecated superseded by {@link BitronixTransactionManagerObjectFactory}.
 */
public class BitronixTransactionManagerFactory implements ObjectFactory {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransactionManagerFactory.class);

    /**
     * Since there can be only one transaction manager per VM instance, this method always returns a reference
     * to the unique BitronixTransactionManager object.
     * @see BitronixTransactionManager
     * @return the unique transaction manager instance
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception {
        if (log.isDebugEnabled()) log.debug("returning the unique transaction manager instance");
        return TransactionManagerServices.getTransactionManager();
    }

}
