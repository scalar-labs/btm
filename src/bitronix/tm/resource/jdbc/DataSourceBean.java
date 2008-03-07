package bitronix.tm.resource.jdbc;

import bitronix.tm.resource.ResourceConfigurationException;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.utils.PropertyUtils;

import java.util.Map;

/**
 * Javabean container for all the properties of a {@link bitronix.tm.resource.jdbc.PoolingDataSource} as configured in the resources
 * configuration file.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @see bitronix.tm.resource.ResourceLoader
 * @author lorban
 * @deprecated This class' functionality has been included in {@link bitronix.tm.resource.jdbc.PoolingDataSource}.
 */
public class DataSourceBean extends ResourceBean {

    private String testQuery;


    /**
     * @return the query that will be used to test connections.
     */
    public String getTestQuery() {
        return testQuery;
    }

    /**
     * When set, the specified query will be executed on the connection acquired from the pool before being handed to
     * the caller. The connections won't be tested when not set.
     * @param testQuery the query that will be used to test connections.
     */
    public void setTestQuery(String testQuery) {
        this.testQuery = testQuery;
    }

    public XAResourceProducer createResource() {
        try {
            PoolingDataSource pds = new PoolingDataSource();
            Map properties = PropertyUtils.getProperties(this);
            PropertyUtils.setProperties(pds, properties);
            pds.init();
            return pds;
        }
        catch (Exception ex) {
            throw new ResourceConfigurationException("cannot create JDBC datasource named " + getUniqueName(), ex);
        }
    }

    /**
     * @return a human-readable String describing the bean.
     */
    public String toString() {
        return "a DataSourceBean with unique name " + getUniqueName() + " and class " + getClassName();
    }
}
