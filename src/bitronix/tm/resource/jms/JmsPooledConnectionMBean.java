package bitronix.tm.resource.jms;

import java.util.Collection;
import java.util.Date;

/**
 * {@link JmsPooledConnection} Management interface.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public interface JmsPooledConnectionMBean {

    String getStateDescription();
    Date getAcquisitionDate();
    Collection getTransactionGtridsCurrentlyHoldingThis();

}
