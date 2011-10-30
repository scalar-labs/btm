package bitronix.tm.resource.jdbc;

import java.sql.Connection;

/**
 * Listener of connections created by a PoolingDataSource. Implementations of this class must be serializable
 * and are handed raw, physical database Connections
 *
 * @author Ludovic Orban
 */
public interface ConnectionCustomizer {

    /**
     * Called when the physical connection is created.
     * @param connection the physical connection.
     * @param uniqueName the PoolingDataSource unique name.
     */
    public void onAcquire(Connection connection, String uniqueName);

    /**
     * Called when the physical connection is destroyed.
     * @param connection the physical connection.
     * @param uniqueName the PoolingDataSource unique name.
     */
    public void onDestroy(Connection connection, String uniqueName);

}
