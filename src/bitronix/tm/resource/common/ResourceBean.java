package bitronix.tm.resource.common;

import java.util.Properties;
import java.io.Serializable;

/**
 * Abstract javabean container for all common properties of a {@link bitronix.tm.resource.common.XAResourceProducer} as configured in the
 * resources configuration file.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public abstract class ResourceBean implements Serializable {

    private String className;
    private String uniqueName;
    private boolean automaticEnlistingEnabled = true;
    private boolean useTmJoin = true;
    private Properties driverProperties = new Properties();
    private int maxPoolSize = 0;
    private int minPoolSize = 0;
    private int maxIdleTime = 60;
    private int acquireIncrement = 1;
    private int acquisitionTimeout = 30;
    private boolean deferConnectionRelease = true;
    private int acquisitionInterval = 1;
    private boolean allowLocalTransactions = false;
    private transient int createdResourcesCounter;

    /**
     * Initialize all properties with their default values.
     */
    protected ResourceBean() {
    }

    /**
     * @return the underlying implementation class name.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Specify the underlying implementation class name of the XA resource described by this bean.
     * @param className the underlying implementation class name.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the resource's unique name.
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Specify the resource unique name to be used to identify this resource during recovery. This name will be
     * registered in the transactions journal so once assigned it must never be changed.
     * @param uniqueName the resource's unique name.
     */
    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    /**
     * @return true if the the XA resource wrapper should enlist/delist this resource automatically in global
     * transactions.
     */
    public boolean getAutomaticEnlistingEnabled() {
        return automaticEnlistingEnabled;
    }

    /**
     * Specify if the XA resource wrapper should enlist/delist this resource automatically in global transactions.
     * When set to false, you have to enlist resources yourself with {@link javax.transaction.Transaction#enlistResource(javax.transaction.xa.XAResource)} and delist them
     * {@link javax.transaction.Transaction#delistResource(javax.transaction.xa.XAResource, int)}.
     * @param automaticEnlistingEnabled true if the the XA resource wrapper should enlist/delist this resource automatically in global
     * transactions.
     */
    public void setAutomaticEnlistingEnabled(boolean automaticEnlistingEnabled) {
        this.automaticEnlistingEnabled = automaticEnlistingEnabled;
    }

    /**
     * @return true if transaction branches joining should be used.
     */
    public boolean getUseTmJoin() {
        return useTmJoin;
    }

    /**
     * Specify if the transaction manager should try to join resources by calling
     * {@link javax.transaction.xa.XAResource#start(javax.transaction.xa.Xid, int)} with
     * {@link javax.transaction.xa.XAResource#TMJOIN}. The transaction manager checks if two branches can be joined by
     * calling {@link javax.transaction.xa.XAResource#isSameRM(javax.transaction.xa.XAResource)}.
     * It should only be set to true if the underlying implementation supports resource joining.
     * @param useTmJoin true if transaction branches joining should be used.
     */
    public void setUseTmJoin(boolean useTmJoin) {
        this.useTmJoin = useTmJoin;
    }

    /**
     * @return the properties that should be set on the underlying implementation.
     */
    public Properties getDriverProperties() {
        return driverProperties;
    }

    /**
     * Set the properties that should be set on the underlying implementation.
     * @param driverProperties the properties that should be set on the underlying implementation.
     */
    public void setDriverProperties(Properties driverProperties) {
        this.driverProperties = driverProperties;
    }

    /**
     * Create the resource wrapper described by this bean.
     * @return the resource wrapper described by this bean.
     * @deprecated superceded by init() method of {@link XAResourceProducer}.
     */
    public abstract XAResourceProducer createResource();

    /**
     * @return the amount of connections to be created in the pool.
     * @deprecated replaced with {@link #getMinPoolSize}.
     */
    public int getPoolSize() {
        return minPoolSize;
    }

    /**
     * Define the amount of connections that should be created in the pool.
     * @param poolSize the amount of connections to be created in the pool.
     * @deprecated replaced with {@link #setMinPoolSize}.
     */
    public void setPoolSize(int poolSize) {
        this.minPoolSize = poolSize;
        this.maxPoolSize = poolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public int getAcquireIncrement() {
        return acquireIncrement;
    }

    public void setAcquireIncrement(int acquireIncrement) {
        this.acquireIncrement = acquireIncrement;
    }

    /**
     * @return the amount of time in seconds.
     */
    public int getAcquisitionTimeout() {
        return acquisitionTimeout;
    }

    /**
     * Define the amount of time in seconds a call to get a connection from the pool will wait when the pool is empty.
     * @param acquisitionTimeout the amount of time in seconds.
     */
    public void setAcquisitionTimeout(int acquisitionTimeout) {
        this.acquisitionTimeout = acquisitionTimeout;
    }

    /**
     * @return true only if the database can run many transactions on the same connection.
     */
    public boolean getDeferConnectionRelease() {
        return deferConnectionRelease;
    }

    /**
     * Define the transaction interleaving capability of the database.
     * Should be true only if the database can run many transactions on the same connection.
     * @param deferConnectionRelease true only if the database can run many transactions on the same connection.
     */
    public void setDeferConnectionRelease(boolean deferConnectionRelease) {
        this.deferConnectionRelease = deferConnectionRelease;
    }

    /**
     * @return the amount of time between failed connection acquirements.
     */
    public int getAcquisitionInterval() {
        return acquisitionInterval;
    }

    /**
     * Set the amount of time in seconds the pool will wait before trying to acquire a connection again after an
     * invalid connection has been attempted to be acquired.
     * @param acquisitionInterval amount of time in seconds.
     */
    public void setAcquisitionInterval(int acquisitionInterval) {
        this.acquisitionInterval = acquisitionInterval;
    }


    /**
     * @return true if the transaction manager should allow mixing XA and non-XA transactions.
     */
    public boolean getAllowLocalTransactions() {
        return allowLocalTransactions;
    }

    /**
     * Set if the transaction manager should allow mixing XA and non-XA transactions. If you know all your transactions
     * should be executed within global (ie: XA) scope it is a good idea to set this property to false.
     * @param allowLocalTransactions if the transaction manager should allow mixing XA and non-XA transactions.
     */
    public void setAllowLocalTransactions(boolean allowLocalTransactions) {
        this.allowLocalTransactions = allowLocalTransactions;
    }

    /**
     * Increment a transient counter. This is used for assigning per-resource numbers to connections.
     * @return the current value of the counter.
     */
    public int incCreatedResourcesCounter() {
        return this.createdResourcesCounter++;
    }

}
