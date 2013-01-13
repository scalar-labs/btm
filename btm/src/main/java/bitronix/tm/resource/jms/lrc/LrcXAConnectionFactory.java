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
package bitronix.tm.resource.jms.lrc;

import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.PropertyUtils;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * XAConnectionFactory implementation for a non-XA JMS resource emulating XA with Last Resource Commit.
 *
 * @author Ludovic Orban
 */
public class LrcXAConnectionFactory implements XAConnectionFactory {

    private volatile String connectionFactoryClassName;
    private volatile Map<String, Object> properties = new HashMap<String, Object>();

    public LrcXAConnectionFactory() {
    }

    public String getConnectionFactoryClassName() {
        return connectionFactoryClassName;
    }

    public void setConnectionFactoryClassName(String connectionFactoryClassName) {
        this.connectionFactoryClassName = connectionFactoryClassName;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public XAConnection createXAConnection() throws JMSException {
        try {
            Class<?> clazz = ClassLoaderUtils.loadClass(connectionFactoryClassName);
            ConnectionFactory nonXaConnectionFactory = (ConnectionFactory) clazz.newInstance();
            PropertyUtils.setProperties(nonXaConnectionFactory, properties);

            return new LrcXAConnection(nonXaConnectionFactory.createConnection());
        } catch (Exception ex) {
            throw (JMSException) new JMSException("unable to connect to non-XA resource " + connectionFactoryClassName).initCause(ex);
        }
    }

    public XAConnection createXAConnection(String user, String password) throws JMSException {
        try {
            Class<?> clazz = ClassLoaderUtils.loadClass(connectionFactoryClassName);
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
