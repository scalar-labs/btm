package bitronix.tm.resource.jms.inbound.simple;

import bitronix.tm.resource.jms.PoolingConnectionFactory;

import javax.jms.JMSException;

/**
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixSimpleInboundPool {

    public BitronixSimpleInboundPool(PoolingConnectionFactory pool, Class messageListenerClass, int poolSize) throws JMSException {
        throw new IllegalArgumentException("not yet implemented");
    }
    
}
