package bitronix.tm.drivers;

import bitronix.tm.resource.jms.ConnectionFactoryBean;
import org.mr.api.jms.MantaXAConnectionFactory;
import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: OrbanL
 * Date: 6-okt-2006
 * Time: 13:39:11
 * To change this template use File | Settings | File Templates.
 */
public class MantarayTest extends TestCase {

    public static ConnectionFactoryBean getConnectionFactoryBean1() {
        System.setProperty("mantaConfig", "/java/mantaray_2.0.1_bin/config/default_config.xml");

        ConnectionFactoryBean bean = new ConnectionFactoryBean();
        bean.setClassName(MantaXAConnectionFactory .class.getName());
        bean.setUniqueName("manta1");
        bean.setPoolSize(2);
        return bean;
    }

}
