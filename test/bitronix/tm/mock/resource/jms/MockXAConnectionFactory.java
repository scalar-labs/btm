package bitronix.tm.mock.resource.jms;

import javax.jms.XAConnectionFactory;
import javax.jms.XAConnection;
import javax.jms.JMSException;

/**
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockXAConnectionFactory implements XAConnectionFactory {

    private String endpoint;

    public XAConnection createXAConnection() throws JMSException {
        return new MockXAConnection();
    }

    public XAConnection createXAConnection(String jndiName, String jndiName1) throws JMSException {
        return new MockXAConnection();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

}
