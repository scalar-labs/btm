package bitronix.tm;

import java.util.Collection;
import java.util.Date;

/**
 * {@link BitronixTransaction} Management interface.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
