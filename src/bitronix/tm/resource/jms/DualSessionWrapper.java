package bitronix.tm.resource.jms;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.BitronixRollbackSystemException;
import bitronix.tm.utils.Decoder;
import bitronix.tm.resource.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.io.Serializable;
import java.util.*;

/**
 * JMS Session wrapper that will send calls to either a XASession or to a non-XA Session depending on the calling
 * context.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class DualSessionWrapper extends AbstractXAResourceHolder implements Session, StateChangeListener {

    private final static Logger log = LoggerFactory.getLogger(DualSessionWrapper.class);

    private JmsPooledConnection pooledConnection;
    private boolean transacted;
    private int acknowledgeMode;

    private XASession xaSession;
    private Session session;
    private XAResource xaResource;
    private MessageListener listener;

    //TODO: shouldn't producers/consumers/subscribers be separated between XA and non-XA session ?
    private Map messageProducers = new HashMap();
    private Map messageConsumers = new HashMap();
    private Map topicSubscribers = new HashMap();

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
            throw new JMSException("session handle is closed");

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

        //TODO: even if delisting fails, requeuing should be done or we'll have a session leak here 

        // delisting
        try {
            TransactionContextHelper.delistFromCurrentTransaction(this, pooledConnection.getPoolingConnectionFactory());
        } catch (BitronixRollbackSystemException ex) {
            throw (JMSException) new JMSException("unilateral rollback of  " + getXAResourceHolderState()).initCause(ex);
        } catch (SystemException ex) {
            throw (JMSException) new JMSException("error delisting " + getXAResourceHolderState()).initCause(ex);
        }

        // requeuing
        try {
            TransactionContextHelper.requeue(this, pooledConnection.getPoolingConnectionFactory());
        } catch (BitronixSystemException ex) {
            throw (JMSException) new JMSException("error delisting " + getXAResourceHolderState()).initCause(ex);
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
        TopicSubscriberWrapper topicSubscriber = (TopicSubscriberWrapper) topicSubscribers.get(key);
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
        TopicSubscriberWrapper topicSubscriber = (TopicSubscriberWrapper) topicSubscribers.get(key);
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
            throw new JMSException("session handle is closed");

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

    /* XAStatefulHolder implementation */

    public List getXAResourceHolders() {
        List holders = new ArrayList(1);
        holders.add(this);
        return holders;
    }

    public Object getConnectionHandle() throws Exception {
        return null;
    }

    /* dumb wrapping of Session methods */

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
            throw new JMSException("cannot commit a resource enlisted in a global transaction");

        getSession().commit();
    }

    public void rollback() throws JMSException {
        if (isParticipatingInActiveGlobalTransaction())
            throw new JMSException("cannot rollback a resource enlisted in a global transaction");

        getSession().rollback();
    }

    public void recover() throws JMSException {
        if (isParticipatingInActiveGlobalTransaction())
            throw new JMSException("cannot recover a resource enlisted in a global transaction");

        getSession().recover();
    }

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

    public QueueBrowser createBrowser(javax.jms.Queue queue) throws JMSException {
        return getSession().createBrowser(queue);
    }

    public QueueBrowser createBrowser(javax.jms.Queue queue, String messageSelector) throws JMSException {
        return getSession().createBrowser(queue, messageSelector);
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

}
