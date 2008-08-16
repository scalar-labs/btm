package bitronix.tm.resource.jms;

import javax.jms.TopicSubscriber;
import javax.jms.Topic;
import javax.jms.JMSException;

/**
 * {@link TopicSubscriber} wrapper that adds XA enlistment semantics.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TopicSubscriberWrapper extends MessageConsumerWrapper implements TopicSubscriber {

    public TopicSubscriberWrapper(TopicSubscriber topicSubscriber, DualSessionWrapper session, PoolingConnectionFactory poolingConnectionFactory) {
        super(topicSubscriber, session, poolingConnectionFactory);
    }

    public Topic getTopic() throws JMSException {
        return ((TopicSubscriber) getMessageConsumer()).getTopic();
    }

    public boolean getNoLocal() throws JMSException {
        return ((TopicSubscriber) getMessageConsumer()).getNoLocal();
    }

    public String toString() {
        return "a TopicSubscriberWrapper of " + session;
    }

}
