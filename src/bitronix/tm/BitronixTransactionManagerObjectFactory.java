package bitronix.tm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Name;
import javax.naming.Context;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * {@link BitronixTransactionManager} object factory for JNDI references.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BitronixTransactionManagerObjectFactory implements ObjectFactory {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransactionManagerObjectFactory.class);

    /**
     * Since there can be only one transaction manager per VM instance, this method always returns a reference
     * to the unique BitronixTransactionManager object.
     * @see BitronixTransactionManager
     * @return the unique transaction manager instance.
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception {
        if (log.isDebugEnabled()) log.debug("returning the unique transaction manager instance");
        return TransactionManagerServices.getTransactionManager();
    }


}
