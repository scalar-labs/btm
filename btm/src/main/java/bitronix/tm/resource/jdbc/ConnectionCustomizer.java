package bitronix.tm.resource.jdbc;

import java.sql.Connection;

/**
 * @author Ludovic Orban
 */
public interface ConnectionCustomizer {

    void onAcquire(Connection c, String uniqueName);

    void onDestroy(Connection c, String uniqueName);

}
