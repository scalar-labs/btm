package bitronix.tm.integration.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 * FactoryBean for PoolingDataSource to correctly manage its lifecycle when used with Spring.
 * 
 * @author Marcus Klimstra (CGI)
 */
public class PoolingDataSourceFactoryBean extends ResourceBean implements FactoryBean<PoolingDataSource>, DisposableBean {
	
	private static final Logger log = LoggerFactory.getLogger(PoolingDataSourceFactoryBean.class);

	private PoolingDataSource ds;

	public Class<?> getObjectType() {
		return PoolingDataSource.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public PoolingDataSource getObject() throws Exception {
		if (ds == null) {
			ds = new PoolingDataSource();
			copySettingsTo(ds);

			log.debug("Initializing PoolingDataSource with id " + ds.getUniqueName());
			ds.init();
		}
		return ds;
	}

	public void destroy() throws Exception {
		log.debug("Closing PoolingDataSource with id " + ds.getUniqueName());
		ds.close();
		ds = null;
	}
}
