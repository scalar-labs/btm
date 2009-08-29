package bitronix.tm.mock.resource.jms;

import javax.jms.*;

/**
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockConnectionFactory implements ConnectionFactory {

    private String endpoint;

    public Connection createConnection() throws JMSException {
        return new MockXAConnection();
    }

    public Connection createConnection(String jndiName, String jndiName1) throws JMSException {
        return new MockXAConnection();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

}