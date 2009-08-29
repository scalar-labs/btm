package bitronix.tm.resource.jms.lrc;

import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.PropertyUtils;

import javax.jms.XAConnectionFactory;
import javax.jms.XAConnection;
import javax.jms.JMSException;
import javax.jms.ConnectionFactory;
import java.util.Properties;

/**
 * XAConnectionFactory implementation for a non-XA JMS resource emulating XA with Last Resource Commit.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class LrcXAConnectionFactory implements XAConnectionFactory {

    private String connectionFactoryClassName;
    private Properties properties = new Properties();

    public LrcXAConnectionFactory() {
    }

    public String getConnectionFactoryClassName() {
        return connectionFactoryClassName;
    }

    public void setConnectionFactoryClassName(String connectionFactoryClassName) {
        this.connectionFactoryClassName = connectionFactoryClassName;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public XAConnection createXAConnection() throws JMSException {
        try {
            Class clazz = ClassLoaderUtils.loadClass(connectionFactoryClassName);
            ConnectionFactory nonXaConnectionFactory = (ConnectionFactory) clazz.newInstance();
            PropertyUtils.setProperties(nonXaConnectionFactory, properties);

            return new LrcXAConnection(nonXaConnectionFactory.createConnection());
        } catch (Exception ex) {
            throw (JMSException) new JMSException("unable to connect to non-XA resource " + connectionFactoryClassName).initCause(ex);
        }
    }

    public XAConnection createXAConnection(String user, String password) throws JMSException {
        try {
            Class clazz = ClassLoaderUtils.loadClass(connectionFactoryClassName);
            ConnectionFactory nonXaConnectionFactory = (ConnectionFactory) clazz.newInstance();
            PropertyUtils.setProperties(nonXaConnectionFactory, properties);

            return new LrcXAConnection(nonXaConnectionFactory.createConnection(user, password));
        } catch (Exception ex) {
            throw (JMSException) new JMSException("unable to connect to non-XA resource " + connectionFactoryClassName).initCause(ex);
        }
    }

    public String toString() {
        return "a JMS LrcXAConnectionFactory on " + connectionFactoryClassName + " with properties " + properties;
    }
}
