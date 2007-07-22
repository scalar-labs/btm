package bitronix.tm.resource.jms.inbound.asf;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.common.TransactionContextHelper;
import bitronix.tm.resource.jms.DualSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixServerSession implements ServerSession, MessageListener {

    private final static Logger log = LoggerFactory.getLogger(BitronixServerSession.class);
    private static int threadCount = 0;


    private DualSessionWrapper session;
    private boolean busy = false;
    private MessageListener listener;
    private BitronixServerSessionThread thread;

    public BitronixServerSession(DualSessionWrapper session, MessageListener listener) {
        this.session = session;
        this.listener = listener;
        synchronized(getClass()) {
            thread = new BitronixServerSessionThread(threadCount++);
        }
        thread.start();
    }

    public Session getSession() throws JMSException {
        return session.getSession(true);
    }

    public void lock() {
        acquireLock(true);
    }

    public void start() throws JMSException {
        if (log.isDebugEnabled()) log.debug("server session started");
        thread.markSessionRunnable();
    }

    private synchronized void acquireLock(boolean lock) {
        busy = lock;
        if (log.isDebugEnabled()) log.debug("server session locked: " + busy);
        notify();
    }

    /**
     * Enlist this connection into the current transaction if automaticEnlistingEnabled = true for this resource.
     * If no transaction is running then this method does nothing.
     * @throws JMSException
     */
    private void enlistResource() throws JMSException {
        if (session.getPoolingConnectionFactory().getAutomaticEnlistingEnabled()) {
            try {
                session.getSession(); // -> needs to be called to init the XAResource
                TransactionContextHelper.enlistInCurrentTransaction(session, session.getPoolingConnectionFactory());
            } catch (SystemException ex) {
                throw (JMSException) new JMSException("error enlisting " + session).initCause(ex);
            } catch (RollbackException ex) {
                throw (JMSException) new JMSException("error enlisting " + session).initCause(ex);
            }
        } // if getAutomaticEnlistingEnabled
    }

    private void delistResource() throws JMSException {
        if (session.getPoolingConnectionFactory().getAutomaticEnlistingEnabled()) {
            // delisting
            try {
                TransactionContextHelper.delistFromCurrentTransaction(session, session.getPoolingConnectionFactory());
            } catch (SystemException ex) {
                throw (JMSException) new JMSException("cannot delist resource " + session).initCause(ex);
            }
        } // if getAutomaticEnlistingEnabled
    }

    public boolean isBusy() {
        return busy;
    }

    public void onMessage(Message message) {
        if (log.isDebugEnabled()) log.debug("internal onMessage start");
        try {
            TransactionManager tm = TransactionManagerServices.getTransactionManager();
            tm.begin();
            enlistResource();
            try {
                if (log.isDebugEnabled()) log.debug("onMessage transaction: " + tm.getTransaction());

                listener.onMessage(message);

            } catch (RuntimeException ex) {
                if (log.isDebugEnabled()) log.debug("caught runtime exception (" + ex + ") - rolling back and rethrowing");
                tm.rollback();
                throw ex;
            } finally {
                delistResource();
            }
            if (tm.getTransaction().getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                if (log.isDebugEnabled()) log.debug("transaction marked rollback only, rolling back: " + tm.getTransaction());
                tm.rollback();
            }
            else {
                if (log.isDebugEnabled()) log.debug("transaction succeeded, committing: " + tm.getTransaction());
                tm.commit();
            }
        } catch (Exception ex) {
            log.error("error in onMessage", ex);
        }
        if (log.isDebugEnabled()) log.debug("internal onMessage end");
    }

    public void close() throws JMSException {
        session.close();
    }

    public String toString() {
        return "a BitronixServerSession on " + session;
    }

    private class BitronixServerSessionThread extends Thread {

        private boolean sessionRunnable = false;

        public BitronixServerSessionThread(int threadNum) {
            setName("bitronix-server-session-" + threadNum);
            setDaemon(true);
        }

        public synchronized void markSessionRunnable() {
            if (log.isDebugEnabled()) log.debug("activating server session thread " + this);
            this.sessionRunnable = true;
            notify();
        }

        public void run() {
            while (true) {
                synchronized (this) {
                    while (!sessionRunnable) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                            // ignore
                        }
                    } // while !sessionRunnable
                } // synchronized

                if (log.isDebugEnabled()) log.debug("server session runnable, starting processing");
                session.run();
                acquireLock(false);
                if (log.isDebugEnabled()) log.debug("server session processing done");
                sessionRunnable = false;
            } // while true
        }

    }

}
