package bitronix.tm.resource.jms;

/**
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public interface PoolingConnectionFactoryMBean {

    public long getInPoolSize();
    public long getTotalPoolSize();
    public void reset() throws Exception;
    
}
