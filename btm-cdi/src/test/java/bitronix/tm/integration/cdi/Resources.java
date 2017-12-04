package bitronix.tm.integration.cdi;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import java.util.Properties;

/**
 * @author aschoerk
 */
public class Resources {

    Logger log = LoggerFactory.getLogger("ResourcesLogger");

    public Resources() {
    }

    @PreDestroy
    public void preDestroyResources() {
        ds.close();
    }

    @Inject
    TransactionManager tm;

    PoolingDataSource ds;

    @Produces
    @ApplicationScoped
    EntityManagerFactory createEntityManagerFactory() {
        return Persistence.createEntityManagerFactory("btm-cdi-test-h2-pu");
    }


    @Produces
    @ApplicationScoped
    DataSource createDataSource() {
        log.info("creating datasource");
        PoolingDataSource res = new PoolingDataSource();
        res.setClassName("org.h2.jdbcx.JdbcDataSource");
        Properties driverProperties = res.getDriverProperties();
        driverProperties.setProperty("URL", "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=0");
        driverProperties.setProperty("user","sa");
        driverProperties.setProperty("password","");
        res.setUniqueName("jdbc/btm-cdi-test-h2");
        res.setMinPoolSize(1);
        res.setMaxPoolSize(3);
        res.init();
        log.info("created  datasource");
        ds = res;
        return res;
    }



}
