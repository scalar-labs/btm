package bitronix.tm.drivers;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import junit.framework.TestCase;
import org.mr.api.jms.MantaXAConnectionFactory;

/**
 * Created by IntelliJ IDEA.
 * User: OrbanL
 * Date: 6-okt-2006
 * Time: 13:39:11
 * To change this template use File | Settings | File Templates.
 */
public class MantarayTest extends TestCase {

    public static PoolingConnectionFactory getPoolingConnectionFactory1() {
        System.setProperty("mantaConfig", "/java/mantaray_2.0.1_bin/config/default_config.xml");

        PoolingConnectionFactory bean = new PoolingConnectionFactory();
        bean.setClassName(MantaXAConnectionFactory .class.getName());
        bean.setUniqueName("manta1");
        bean.setPoolSize(2);
        return bean;
    }

}
