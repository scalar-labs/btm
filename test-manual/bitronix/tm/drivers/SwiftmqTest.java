package bitronix.tm.drivers;

import bitronix.tm.resource.jms.ConnectionFactoryBean;
import bitronix.tm.resource.jms.JndiXAConnectionFactory;
import bitronix.tm.mock.resource.MockXid;
import bitronix.tm.internal.Decoder;
import junit.framework.TestCase;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.jms.*;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;
import java.util.Properties;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: OrbanL
 * Date: 6-okt-2006
 * Time: 13:39:11
 * To change this template use File | Settings | File Templates.
 */
public class SwiftmqTest extends TestCase {

    public static ConnectionFactoryBean getConnectionFactoryBean1() {
        ConnectionFactoryBean bean = new ConnectionFactoryBean();
        bean.setClassName(JndiXAConnectionFactory.class.getName());
        bean.setUniqueName("swiftmq1");
        bean.setPoolSize(5);
        bean.getDriverProperties().put("initialContextFactory", "com.swiftmq.jndi.InitialContextFactoryImpl");
        bean.getDriverProperties().put("providerUrl", "smqp://localhost:4001/timeout=10000");
        bean.getDriverProperties().put("name", "QueueConnectionFactory");
        return bean;
    }

    public static ConnectionFactoryBean getConnectionFactoryBean2() {
        ConnectionFactoryBean bean = new ConnectionFactoryBean();
        bean.setClassName(JndiXAConnectionFactory.class.getName());
        bean.setUniqueName("swiftmq2");
        bean.setPoolSize(5);
        bean.getDriverProperties().put("initialContextFactory", "com.swiftmq.jndi.InitialContextFactoryImpl");
        bean.getDriverProperties().put("providerUrl", "smqp://localhost:4002/timeout=10000");
        bean.getDriverProperties().put("name", "QueueConnectionFactory");
        return bean;
    }


    // expected to fail, see: http://www.nabble.com/JMS-XA-pool-tf2652508.html#a7510690
    public void test2() throws Exception {
        XAConnectionFactory xacf = getXACF();

        XAConnection xac = xacf.createXAConnection();
        XASession xas = xac.createXASession();

        for (int i=0;i<100;i++) {
            try {
                MessageProducer mp = xas.createProducer(xas.createQueue("queue-testAutoEnlistment"));
                mp.close();
            } catch(Exception ex) {
                System.out.println("i="+i);
                throw ex;
            }
        }

        xas.close();
        xac.close();
    }

    // expected to fail, see: http://www.nabble.com/JMS-XA-pool-tf2652508.html#a7510690
    public void test1() throws Exception {
        try {
            XAConnectionFactory xacf = getXACF();
            Xid xid = new MockXid(0, 0);

            XAConnection xac = xacf.createXAConnection();
            XASession xas = xac.createXASession();
            XAResource xar = xas.getXAResource();

            xas.close();
            xar.commit(xid, true);

            xac.close();
        } catch (XAException ex) {
            // output must be dumped to a file b/c of a copy/paste bug in IDEA...
//            PrintWriter pw = new PrintWriter(new FileOutputStream("c:/out"));
//            pw.println("XAException caught, error code=" + Decoder.decodeXAExceptionErrorCode(ex));
//            ex.printStackTrace(pw);
//            pw.close();
            throw ex;
        }
    }

    public void test3() throws Exception {
        try {
            XAConnectionFactory xacf = getXACF();
            Xid xid = new MockXid(2, 0);

            XAConnection xac = xacf.createXAConnection();
            XASession xas = xac.createXASession();
            XAResource xar = xas.getXAResource();

            xar.start(xid, XAResource.TMNOFLAGS);
            xar.end(xid, XAResource.TMSUCCESS);

            int rc = xar.prepare(xid);
            System.out.println("rc=" + Decoder.decodePrepareVote(rc));

            xas.close();
            xac.close();
        } catch (XAException ex) {
            System.err.println("XAException caught, error code=" + Decoder.decodeXAExceptionErrorCode(ex));
            throw ex;
        }
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

    public void test5() throws Exception {
        XAConnectionFactory xacf = getXACF();
        Xid xid = new MockXid(0, 0);

        XAConnection xac = xacf.createXAConnection();
        XASession xas = xac.createXASession();
        XAResource xar = xas.getXAResource();

        xar.start(xid, XAResource.TMNOFLAGS);
        sendmsg(xas, "aaa");
        xar.end(xid, XAResource.TMSUCCESS);

        xar.commit(xid, true);

        xas.close();
        xac.close();
    }

    private void sendmsg(XASession session1, String msg) throws JMSException {
        Queue queue = session1.createQueue("queue-testAutoEnlistment");
        MessageProducer producer1 = session1.createProducer(queue);

        producer1.send(session1.createTextMessage(msg + " - " + new Date()));

        producer1.close();
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

    private void forget(XAResource xar, Xid[] xids) throws XAException {
        System.out.println("forget " + xids.length);
        for (int i = 0; i < xids.length; i++) {
            Xid xid = xids[i];
            xar.forget(xid);
        }
    }

    private void commit(XAResource xar, Xid[] xids) throws XAException {
        System.out.println("commit " + xids.length);
        for (int i = 0; i < xids.length; i++) {
            Xid xid = xids[i];
            try {
                xar.commit(xid, false);
            } catch (XAException ex) {
                if (ex.errorCode == XAException.XA_HEURCOM) {
                    System.out.println("forget " + xids.length);
                    xar.forget(xid);
                }
                else throw ex;
            }
        }
    }


    private XAConnectionFactory getXACF() throws NamingException {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.swiftmq.jndi.InitialContextFactoryImpl");
        env.put(Context.PROVIDER_URL, "smqp://localhost:4001/timeout=10000");
        Context ctx = new InitialContext(env);
        XAConnectionFactory xacf = (XAConnectionFactory) ctx.lookup("QueueConnectionFactory");
        return xacf;
    }

}
