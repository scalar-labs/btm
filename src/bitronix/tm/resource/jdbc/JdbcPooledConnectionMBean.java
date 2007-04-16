package bitronix.tm.resource.jdbc;

import java.util.Date;

/**
 * {@link JdbcPooledConnection} Management interface.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public interface JdbcPooledConnectionMBean {

    String getStateDescription();
    Date getAcquisitionDate();
    String getTransactionGtridCurrentlyHoldingThis();
    
}
