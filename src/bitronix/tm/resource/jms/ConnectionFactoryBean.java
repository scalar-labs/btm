package bitronix.tm.resource.jms;

import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.ResourceConfigurationException;

import java.util.Properties;

/**
 * Javabean container for all the properties of a {@link bitronix.tm.resource.jms.PoolingConnectionFactory} as configured in the resources
 * configuration file.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @see bitronix.tm.resource.ResourceLoader
 * @author lorban
 */
public class ConnectionFactoryBean extends ResourceBean {

    private Properties serverSessionPool = new Properties();
    private boolean cacheProducersConsumers = true;
    private boolean testConnections = false;

    public XAResourceProducer createResource() {
        try {
            return new PoolingConnectionFactory(this);
        }
        catch (Exception ex) {
            throw new ResourceConfigurationException("cannot create JMS connection factory named " + getUniqueName(), ex);
        }
    }

    public Properties getServerSessionPool() {
        return serverSessionPool;
    }

    public void setServerSessionPool(Properties serverSessionPool) {
        this.serverSessionPool = serverSessionPool;
    }

    public boolean getCacheProducersConsumers() {
        return cacheProducersConsumers;
    }

    public void setCacheProducersConsumers(boolean cacheProducersConsumers) {
        this.cacheProducersConsumers = cacheProducersConsumers;
    }

    public boolean getTestConnections() {
        return testConnections;
    }

    public void setTestConnections(boolean testConnections) {
        this.testConnections = testConnections;
    }

    /**
     * @return a human-readable String describing the bean.
     */
    public String toString() {
        return "a ConnectionFactoryBean with " + (getConfigurationName() != null ? "configured name " + getConfigurationName() + ", " : "") + "unique name " + getUniqueName() + " and class " + getClassName();
    }

}
