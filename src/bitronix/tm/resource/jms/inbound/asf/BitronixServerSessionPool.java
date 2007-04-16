package bitronix.tm.resource.jms.inbound.asf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.List;
import java.util.ArrayList;

import bitronix.tm.resource.jms.JmsConnectionHandle;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import bitronix.tm.resource.jms.DualSessionWrapper;

/**
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixServerSessionPool implements ServerSessionPool {

    private final static Logger log = LoggerFactory.getLogger(BitronixServerSessionPool.class);

    private List bitronixServerSessions = new ArrayList();
    private JmsConnectionHandle connection;

    public BitronixServerSessionPool(PoolingConnectionFactory pool, Class messageListenerClass, int poolSize) throws JMSException {
        try {
            if (log.isDebugEnabled()) log.debug("getting connection for server session pool");
            connection = (JmsConnectionHandle) pool.createConnection();
            if (log.isDebugEnabled()) log.debug("filling server session pool with " + poolSize + " session(s)");
            for (int i=0; i<poolSize ;i++) {
                MessageListener listener = (MessageListener) messageListenerClass.newInstance();

                DualSessionWrapper session = (DualSessionWrapper) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                BitronixServerSession serverSession = new BitronixServerSession(session, listener);
                session.setMessageListener(serverSession);
                if (log.isDebugEnabled()) log.debug("created server session " + serverSession);
                bitronixServerSessions.add(serverSession);
            }
            connection.start();
        } catch (InstantiationException ex) {
            throw (JMSException) new JMSException("unable to instantiate messageListenerClass " + messageListenerClass.getName()).initCause(ex);
        } catch (IllegalAccessException ex) {
            throw (JMSException) new JMSException("unable to instantiate messageListenerClass " + messageListenerClass.getName()).initCause(ex);
        } catch (ClassCastException ex) {
            throw (JMSException) new JMSException("configured messageListenerClass is not subtype of javax.jms.MessageListener").initCause(ex);
        }
    }

    public ServerSession getServerSession() throws JMSException {
        if (log.isDebugEnabled()) log.debug("acquiring server session");
        synchronized (bitronixServerSessions) {
            while (true) {
                if (log.isDebugEnabled()) log.debug("checking " + bitronixServerSessions.size() + " server session(s) to find one available...");
                for (int i = 0; i < bitronixServerSessions.size(); i++) {
                    BitronixServerSession serverSession = (BitronixServerSession) bitronixServerSessions.get(i);
                    if (!serverSession.isBusy()) {
                        serverSession.lock();
                        if (log.isDebugEnabled()) log.debug("server session acquired: " + serverSession);
                        return serverSession;
                    }
                }

                try {
                    if (log.isDebugEnabled()) log.debug("no available server session found in pool, waiting a bit before retrying...");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
            } // while
        } // sync
    }

    public void close() throws JMSException {
        if (log.isDebugEnabled()) log.debug("closing " + this);
        for (int i = 0; i < bitronixServerSessions.size(); i++) {
            BitronixServerSession bitronixServerSession = (BitronixServerSession) bitronixServerSessions.get(i);
            bitronixServerSession.close();
        }
        connection.close();
    }

    public String toString() {
        return "a BitronixServerSessionPool with " + bitronixServerSessions.size() + " session(s) on " + connection;
    }
}
