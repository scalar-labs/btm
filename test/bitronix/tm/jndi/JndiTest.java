package bitronix.tm.jndi;

import junit.framework.TestCase;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 * Created by IntelliJ IDEA.
 * User: lorban
 * Date: 24 mars 2008
 * Time: 18:44:31
 * To change this template use File | Settings | File Templates.
 */
public class JndiTest extends TestCase {

    private BitronixTransactionManager transactionManager;

    protected void setUp() throws Exception {
        transactionManager = TransactionManagerServices.getTransactionManager();
    }

    protected void tearDown() throws Exception {
        transactionManager.shutdown();
    }

    public void testDefaultUserTransactionAndResources() throws Exception {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setMaxPoolSize(1);
        pds.setClassName(MockXADataSource.class.getName());
        pds.setUniqueName("jdbc/pds");
        pds.init();

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, BitronixInitialContextFactory.class.getName());
        Context ctx = new InitialContext(env);

        assertTrue(transactionManager == ctx.lookup("java:comp/UserTransaction"));
        assertNull(ctx.lookup("aaa"));
        assertTrue(pds == ctx.lookup("jdbc/pds"));

        ctx.close();

        pds.close();
    }

    public void testSpecialUserTransactionName() throws Exception {
        transactionManager.shutdown();
        TransactionManagerServices.getConfiguration().setJndiUserTransactionName("TM");
        transactionManager = TransactionManagerServices.getTransactionManager();

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, BitronixInitialContextFactory.class.getName());
        Context ctx = new InitialContext(env);

        assertNull(ctx.lookup("java:comp/UserTransaction"));
        assertTrue(transactionManager == ctx.lookup("TM"));
        assertNull(ctx.lookup("aaa"));

        ctx.close();
    }

}
