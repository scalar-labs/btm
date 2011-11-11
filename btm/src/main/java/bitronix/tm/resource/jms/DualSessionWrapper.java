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
package bitronix.tm.resource.jms;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.internal.BitronixRollbackSystemException;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.StateChangeListener;
import bitronix.tm.resource.common.TransactionContextHelper;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.utils.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.jms.TransactionInProgressException;
import javax.jms.TransactionRolledBackException;
import javax.jms.XASession;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JMS Session wrapper that will send calls to either a XASession or to a non-XA Session depending on the calling
 * context.
 *
 * @author lorban
 */
public class DualSessionWrapper extends AbstractXAResourceHolder implements Session, StateChangeListener {

    private final static Logger log = LoggerFactory.getLogger(DualSessionWrapper.class);

    private final JmsPooledConnection pooledConnection;
    private final boolean transacted;
    private final int acknowledgeMode;

    private XASession xaSession;
    private Session session;
    private XAResource xaResource;
    private MessageListener listener;

    //TODO: shouldn't producers/consumers/subscribers be separated between XA and non-XA session ?
    private final Map<MessageProducerConsumerKey, MessageProducer> messageProducers = new HashMap<MessageProducerConsumerKey, MessageProducer>();
    private final Map<MessageProducerConsumerKey, MessageConsumer> messageConsumers = new HashMap<MessageProducerConsumerKey, MessageConsumer>();
    private final Map<MessageProducerConsumerKey, TopicSubscriberWrapper> topicSubscribers = new HashMap<MessageProducerConsumerKey, TopicSubscriberWrapper>();

    public DualSessionWrapper(JmsPooledConnection pooledConnection, boolean transacted, int acknowledgeMode) {
        this.pooledConnection = pooledConnection;
        this.transacted = transacted;
        this.acknowledgeMode = acknowledgeMode;

        if (log.isDebugEnabled()) log.debug("getting session handle from " + pooledConnection);
        setState(STATE_ACCESSIBLE);
        addStateChangeEventListener(this);
    }

    public PoolingConnectionFactory getPoolingConnectionFactory() {
        return pooledConnection.getPoolingConnectionFactory();
    }

    public Session getSession() throws JMSException {
        return getSession(false);
    }

    public Session getSession(boolean forceXa) throws JMSException {
        if (getState() == STATE_CLOSED)
            throw new IllegalStateException("session handle is closed");

        if (forceXa) {
            if (log.isDebugEnabled()) log.debug("choosing XA session (forced)");
            return createXASession();
        }
        else {
            BitronixTransaction currentTransaction = TransactionContextHelper.currentTransaction();
            if (currentTransaction != null) {
                if (log.isDebugEnabled()) log.debug("choosing XA session");
                return createXASession();
            }
            if (log.isDebugEnabled()) log.debug("choosing non-XA session");
            return createNonXASession();
        }
    }

    private Session createNonXASession() throws JMSException {
        // non-XA
        if (session == null) {
            session = pooledConnection.getXAConnection().createSession(transacted, acknowledgeMode);
            if (listener != null) {
                session.setMessageListener(listener);
                if (log.isDebugEnabled()) log.debug("get non-XA session registered message listener: " + listener);
            }
        }
        return session;
    }

    private Session createXASession() throws JMSException {
        // XA
        if (xaSession == null) {
            xaSession = pooledConnection.getXAConnection().createXASession();
            if (listener != null) {
                xaSession.setMessageListener(listener);
                if (log.isDebugEnabled()) log.debug("get XA session registered message listener: " + listener);
            }
            xaResource = xaSession.getXAResource();
        }
        return xaSession.getSession();
    }

    public String toString() {
        return "a DualSessionWrapper in state " + Decoder.decodeXAStatefulHolderState(getState()) + " of " + pooledConnection;
    }


    /* wrapped Session methods that have special XA semantics */

    public void close() throws JMSException {
        if (getState() != STATE_ACCESSIBLE) {
            if (log.isDebugEnabled()) log.debug("not closing already closed " + this);
            return;
        }

        if (log.isDebugEnabled()) log.debug("closing " + this);

        // delisting
        try {
            TransactionContextHelper.delistFromCurrentTransaction(this);
        }
        catch (BitronixRollbackSystemException ex) {
            throw (JMSException) new TransactionRolledBackException("unilateral rollback of " + this).initCause(ex);
        }
        catch (SystemException ex) {
            throw (JMSException) new JMSException("error delisting " + this).initCause(ex);
        }
        finally {
            // requeuing
            try {
                TransactionContextHelper.requeue(this, pooledConnection.getPoolingConnectionFactory());
            }
            catch (BitronixSystemException ex) {
                // this may hide the exception thrown by delistFromCurrentTransaction() but
                // an error requeuing must absolutely be reported as an exception.
                // Too bad if this happens... See JdbcPooledConnection.release() as well.
                throw (JMSException) new JMSException("error requeuing " + this).initCause(ex);
            }
        }

    }

    public Date getLastReleaseDate() {
        return null;
    }

    /*
     * When the session is closed (directly or deferred) the action is to change its state to IN_POOL.
     * There is no such state for JMS sessions, this just means that it has been closed -> force a
     * state switch to CLOSED then clean up.
     */
    public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
        if (newState == STATE_IN_POOL) {
            setState(STATE_CLOSED);
        }
        else if (newState == STATE_CLOSED) {
            if (log.isDebugEnabled()) log.debug("session state changing to CLOSED, cleaning it up: " + this);

            if (xaSession != null) {
                try {
                    xaSession.close();
                } catch (JMSException ex) {
                    log.error("error closing XA session", ex);
                }
                xaSession = null;
                xaResource = null;
            }

            if (session != null) {
                try {
                    session.close();
                } catch (JMSException ex) {
                    log.error("error closing session", ex);
                }
                session = null;
            }

            Iterator it = messageProducers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                MessageProducerWrapper messageProducerWrapper = (MessageProducerWrapper) entry.getValue();
                try {
                    messageProducerWrapper.close();
                } catch (JMSException ex) {
                    log.error("error closing message producer", ex);
                }
            }
            messageProducers.clear();

            it = messageConsumers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                MessageConsumerWrapper messageConsumerWrapper = (MessageConsumerWrapper) entry.getValue();
                try {
                    messageConsumerWrapper.close();
                } catch (JMSException ex) {
                    log.error("error closing message consumer", ex);
                }
            }
            messageConsumers.clear();

        } // if newState == STATE_CLOSED
    }

    public void stateChanging(XAStatefulHolder source, int currentState, int futureState) {
    }

    public MessageProducer createProducer(Destination destination) throws JMSException {
        MessageProducerConsumerKey key = new MessageProducerConsumerKey(destination);
        if (log.isDebugEnabled()) log.debug("looking for producer based on " + key);
        MessageProducerWrapper messageProducer = (MessageProducerWrapper) messageProducers.get(key);
        if (messageProducer == null) {
            if (log.isDebugEnabled()) log.debug("found no producer based on " + key + ", creating it");
            messageProducer = new MessageProducerWrapper(getSession().createProducer(destination), this, pooledConnection.getPoolingConnectionFactory());

            if (pooledConnection.getPoolingConnectionFactory().getCacheProducersConsumers()) {
                if (log.isDebugEnabled()) log.debug("caching producer via key " + key);
                messageProducers.put(key, messageProducer);
            }
        }
        else if (log.isDebugEnabled()) log.debug("found producer based on " + key + ", recycling it: " + messageProducer);
        return messageProducer;
    }

    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        MessageProducerConsumerKey key = new MessageProducerConsumerKey(destination);
        if (log.isDebugEnabled()) log.debug("looking for consumer based on " + key);
        MessageConsumerWrapper messageConsumer = (MessageConsumerWrapper) messageConsumers.get(key);
        if (messageConsumer == null) {
            if (log.isDebugEnabled()) log.debug("found no consumer based on " + key + ", creating it");
            messageConsumer = new MessageConsumerWrapper(getSession().createConsumer(destination), this, pooledConnection.getPoolingConnectionFactory());

            if (pooledConnection.getPoolingConnectionFactory().getCacheProducersConsumers()) {
                if (log.isDebugEnabled()) log.debug("caching consumer via key " + key);
                messageConsumers.put(key, messageConsumer);
            }
        }
        else if (log.isDebugEnabled()) log.debug("found consumer based on " + key + ", recycling it: " + messageConsumer);
        return messageConsumer;
    }

    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
        MessageProducerConsumerKey key = new MessageProducerConsumerKey(destination, messageSelector);
        if (log.isDebugEnabled()) log.debug("looking for consumer based on " + key);
        MessageConsumerWrapper messageConsumer = (MessageConsumerWrapper) messageConsumers.get(key);
        if (messageConsumer == null) {
            if (log.isDebugEnabled()) log.debug("found no consumer based on " + key + ", creating it");
            messageConsumer = new MessageConsumerWrapper(getSession().createConsumer(destination, messageSelector), this, pooledConnection.getPoolingConnectionFactory());

            if (pooledConnection.getPoolingConnectionFactory().getCacheProducersConsumers()) {
                if (log.isDebugEnabled()) log.debug("caching consumer via key " + key);
                messageConsumers.put(key, messageConsumer);
            }
        }
        else if (log.isDebugEnabled()) log.debug("found consumer based on " + key + ", recycling it: " + messageConsumer);
        return messageConsumer;
    }

    public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSException {
        MessageProducerConsumerKey key = new MessageProducerConsumerKey(destination, messageSelector, noLocal);
        if (log.isDebugEnabled()) log.debug("looking for consumer based on " + key);
        MessageConsumerWrapper messageConsumer = (MessageConsumerWrapper) messageConsumers.get(key);
        if (messageConsumer == null) {
            if (log.isDebugEnabled()) log.debug("found no consumer based on " + key + ", creating it");
            messageConsumer = new MessageConsumerWrapper(getSession().createConsumer(destination, messageSelector, noLocal), this, pooledConnection.getPoolingConnectionFactory());

            if (pooledConnection.getPoolingConnectionFactory().getCacheProducersConsumers()) {
                if (log.isDebugEnabled()) log.debug("caching consumer via key " + key);
                messageConsumers.put(key, messageConsumer);
            }
        }
        else if (log.isDebugEnabled()) log.debug("found consumer based on " + key + ", recycling it: " + messageConsumer);
        return messageConsumer;
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        MessageProducerConsumerKey key = new MessageProducerConsumerKey(topic);
        if (log.isDebugEnabled()) log.debug("looking for durable subscriber based on " + key);
        TopicSubscriberWrapper topicSubscriber = topicSubscribers.get(key);
        if (topicSubscriber == null) {
            if (log.isDebugEnabled()) log.debug("found no durable subscriber based on " + key + ", creating it");
            topicSubscriber = new TopicSubscriberWrapper(getSession().createDurableSubscriber(topic, name), this, pooledConnection.getPoolingConnectionFactory());

            if (pooledConnection.getPoolingConnectionFactory().getCacheProducersConsumers()) {
                if (log.isDebugEnabled()) log.debug("caching durable subscriber via key " + key);
                topicSubscribers.put(key, topicSubscriber);
            }
        }
        else if (log.isDebugEnabled()) log.debug("found durable subscriber based on " + key + ", recycling it: " + topicSubscriber);
        return topicSubscriber;
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        MessageProducerConsumerKey key = new MessageProducerConsumerKey(topic, messageSelector, noLocal);
        if (log.isDebugEnabled()) log.debug("looking for durable subscriber based on " + key);
        TopicSubscriberWrapper topicSubscriber = topicSubscribers.get(key);
        if (topicSubscriber == null) {
            if (log.isDebugEnabled()) log.debug("found no durable subscriber based on " + key + ", creating it");
            topicSubscriber = new TopicSubscriberWrapper(getSession().createDurableSubscriber(topic, name, messageSelector, noLocal), this, pooledConnection.getPoolingConnectionFactory());

            if (pooledConnection.getPoolingConnectionFactory().getCacheProducersConsumers()) {
                if (log.isDebugEnabled()) log.debug("caching durable subscriber via key " + key);
                topicSubscribers.put(key, topicSubscriber);
            }
        }
        else if (log.isDebugEnabled()) log.debug("found durable subscriber based on " + key + ", recycling it: " + topicSubscriber);
        return topicSubscriber;
    }

    public MessageListener getMessageListener() throws JMSException {
        return listener;
    }

    public void setMessageListener(MessageListener listener) throws JMSException {
        if (getState() == STATE_CLOSED)
            throw new IllegalStateException("session handle is closed");

        if (session != null)
            session.setMessageListener(listener);
        if (xaSession != null)
            xaSession.setMessageListener(listener);

        this.listener = listener;
    }

    public void run() {
        try {
            Session session = getSession(true);
            if (log.isDebugEnabled()) log.debug("running XA session " + session);
            session.run();
        } catch (JMSException ex) {
            log.error("error getting session", ex);
        }
    }

    /* XAResourceHolder implementation */

    public XAResource getXAResource() {
        return xaResource;
    }

    public ResourceBean getResourceBean() {
        return getPoolingConnectionFactory();
    }

    /* XAStatefulHolder implementation */

    public List<XAResourceHolder> getXAResourceHolders() {
        return Arrays.asList((XAResourceHolder) this);
    }

    public Object getConnectionHandle() throws Exception {
        return null;
    }

    /* XA-enhanced methods */

    public boolean getTransacted() throws JMSException {
        if (isParticipatingInActiveGlobalTransaction())
            return true; // for consistency with EJB 2.1 spec (17.3.5)

        return getSession().getTransacted();
    }

    public int getAcknowledgeMode() throws JMSException {
        if (isParticipatingInActiveGlobalTransaction())
            return 0; // for consistency with EJB 2.1 spec (17.3.5)

        return getSession().getAcknowledgeMode();
    }

    public void commit() throws JMSException {
        if (isParticipatingInActiveGlobalTransaction())
            throw new TransactionInProgressException("cannot commit a resource enlisted in a global transaction");

        getSession().commit();
    }

    public void rollback() throws JMSException {
        if (isParticipatingInActiveGlobalTransaction())
            throw new TransactionInProgressException("cannot rollback a resource enlisted in a global transaction");

        getSession().rollback();
    }

    public void recover() throws JMSException {
        if (isParticipatingInActiveGlobalTransaction())
            throw new TransactionInProgressException("cannot recover a resource enlisted in a global transaction");

        getSession().recover();
    }

    public QueueBrowser createBrowser(javax.jms.Queue queue) throws JMSException {
        enlistResource();
        return getSession().createBrowser(queue);
    }

    public QueueBrowser createBrowser(javax.jms.Queue queue, String messageSelector) throws JMSException {
        enlistResource();
        return getSession().createBrowser(queue, messageSelector);
    }

    /* dumb wrapping of Session methods */

    public BytesMessage createBytesMessage() throws JMSException {
        return getSession().createBytesMessage();
    }

    public MapMessage createMapMessage() throws JMSException {
        return getSession().createMapMessage();
    }

    public Message createMessage() throws JMSException {
        return getSession().createMessage();
    }

    public ObjectMessage createObjectMessage() throws JMSException {
        return getSession().createObjectMessage();
    }

    public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException {
        return getSession().createObjectMessage(serializable);
    }

    public StreamMessage createStreamMessage() throws JMSException {
        return getSession().createStreamMessage();
    }

    public TextMessage createTextMessage() throws JMSException {
        return getSession().createTextMessage();
    }

    public TextMessage createTextMessage(String text) throws JMSException {
        return getSession().createTextMessage(text);
    }

    public javax.jms.Queue createQueue(String queueName) throws JMSException {
        return getSession().createQueue(queueName);
    }

    public Topic createTopic(String topicName) throws JMSException {
        return getSession().createTopic(topicName);
    }

    public TemporaryQueue createTemporaryQueue() throws JMSException {
        return getSession().createTemporaryQueue();
    }

    public TemporaryTopic createTemporaryTopic() throws JMSException {
        return getSession().createTemporaryTopic();
    }

    public void unsubscribe(String name) throws JMSException {
        getSession().unsubscribe(name);
    }


    /**
     * Enlist this session into the current transaction if automaticEnlistingEnabled = true for this resource.
     * If no transaction is running then this method does nothing.
     * @throws JMSException
     */
    protected void enlistResource() throws JMSException {
        PoolingConnectionFactory poolingConnectionFactory = pooledConnection.getPoolingConnectionFactory();
        if (poolingConnectionFactory.getAutomaticEnlistingEnabled()) {
            getSession(); // make sure the session is created before enlisting it
            try {
                TransactionContextHelper.enlistInCurrentTransaction(this);
            } catch (SystemException ex) {
                throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
            } catch (RollbackException ex) {
                throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
            }
        } // if getAutomaticEnlistingEnabled
    }

}
