package bitronix.tm.resource.jdbc;

/**
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public interface PoolingDataSourceMBean {

    public long getInPoolSize();
    public long getTotalPoolSize();
    public void reset() throws Exception;

}
