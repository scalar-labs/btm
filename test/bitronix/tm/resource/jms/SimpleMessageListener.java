package bitronix.tm.resource.jms;

import javax.jms.MessageListener;
import javax.jms.Message;

/**
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class SimpleMessageListener implements MessageListener {
    public void onMessage(Message message) {
        System.out.println(message);
    }
}
