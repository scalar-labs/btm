package bitronix.tm.drivers;

import bitronix.tm.internal.Decoder;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import junit.framework.TestCase;
import org.apache.activemq.ActiveMQXAConnectionFactory;

import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Created by IntelliJ IDEA.
 * User: OrbanL
 * Date: 6-okt-2006
 * Time: 13:39:11
 * To change this template use File | Settings | File Templates.
 */
public class AmqTest extends TestCase {

    public static PoolingConnectionFactory getPoolingConnectionFactory1() {
        PoolingConnectionFactory bean = new PoolingConnectionFactory();
        bean.setClassName(ActiveMQXAConnectionFactory.class.getName());
        bean.setUniqueName("amq1");
        bean.setPoolSize(5);
        bean.getDriverProperties().put("brokerURL", "tcp://localhost:61616");
        return bean;
    }

    public static PoolingConnectionFactory getPoolingConnectionFactory2() {
        PoolingConnectionFactory bean = new PoolingConnectionFactory();
        bean.setClassName(ActiveMQXAConnectionFactory.class.getName());
        bean.setUniqueName("amq2");
        bean.setPoolSize(5);
        bean.getDriverProperties().put("brokerURL", "tcp://localhost:61616");
        return bean;
    }

    public void test4() throws Exception {
        try {
            XAConnectionFactory xacf = getXACF();

            XAConnection xac = xacf.createXAConnection();
            XASession xas = xac.createXASession();
            XAResource xar = xas.getXAResource();

            Xid[] xids = xar.recover(XAResource.TMSTARTRSCAN);
            rollback(xar, xids);
            while (xids.length > 0) {
                xids = xar.recover(XAResource.TMNOFLAGS);
                rollback(xar, xids);
            }
            xids = xar.recover(XAResource.TMENDRSCAN);
            rollback(xar, xids);

            xas.close();
            xac.close();
        } catch (XAException ex) {
            System.err.println("XAException caught, error code=" + Decoder.decodeXAExceptionErrorCode(ex));
            throw ex;
        }
    }
    private void rollback(XAResource xar, Xid[] xids) throws XAException {
        System.out.println("rollback " + xids.length);
        for (int i = 0; i < xids.length; i++) {
            Xid xid = xids[i];
            try {
                xar.rollback(xid);
            } catch (XAException ex) {
                if (ex.errorCode == XAException.XA_HEURRB) {
                    System.out.println("forget " + xids.length);
                    xar.forget(xid);
                }
                else throw ex;
            }
        }
    }

    private XAConnectionFactory getXACF() {
        ActiveMQXAConnectionFactory xacf = new ActiveMQXAConnectionFactory();
        xacf.setBrokerURL("tcp://localhost:61616");
        return xacf;
    }


}
