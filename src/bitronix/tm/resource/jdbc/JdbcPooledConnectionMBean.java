package bitronix.tm.resource.jdbc;

import java.util.Date;

/**
 * {@link JdbcPooledConnection} Management interface.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public interface JdbcPooledConnectionMBean {

    String getStateDescription();
    Date getAcquisitionDate();
    String getTransactionGtridCurrentlyHoldingThis();
    
}
