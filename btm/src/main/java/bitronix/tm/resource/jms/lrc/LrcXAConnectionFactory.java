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
package bitronix.tm.resource.jms.lrc;

import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.PropertyUtils;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import java.util.Properties;

/**
 * XAConnectionFactory implementation for a non-XA JMS resource emulating XA with Last Resource Commit.
 *
 * @author lorban
 */
public class LrcXAConnectionFactory implements XAConnectionFactory {

    private volatile String connectionFactoryClassName;
    private volatile Properties properties = new Properties();

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
