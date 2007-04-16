package bitronix.tm.resource.jms;

import java.util.Collection;
import java.util.Date;

/**
 * {@link JmsPooledConnection} Management interface.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public interface JmsPooledConnectionMBean {

    String getStateDescription();
    Date getAcquisitionDate();
    Collection getTransactionGtridsCurrentlyHoldingThis();

}
