package bitronix.tm;

import java.util.Collection;
import java.util.Date;

/**
 * {@link BitronixTransaction} Management interface.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public interface BitronixTransactionMBean {

    String getGtrid();
    String getStatusDescription();
    String getThreadName();
    Date getStartDate();
    Collection getEnlistedResourcesUniqueNames();

}
