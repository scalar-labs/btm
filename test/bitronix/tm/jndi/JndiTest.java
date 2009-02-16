package bitronix.tm.jndi;

import junit.framework.TestCase;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import java.util.Hashtable;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.jdbc.PoolingDataSource;


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

        try {
            ctx.lookup("aaa");
            fail("expected NameNotFoundException");
        } catch (NameNotFoundException ex) {
            assertEquals("unable to find a bound object at name 'aaa'", ex.getMessage());
        }

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


        try {
            ctx.lookup("java:comp/UserTransaction");
            fail("expected NameNotFoundException");
        } catch (NameNotFoundException ex) {
            assertEquals("unable to find a bound object at name 'java:comp/UserTransaction'", ex.getMessage());
        }


        assertTrue(transactionManager == ctx.lookup("TM"));

        try {
            ctx.lookup("aaa");
            fail("expected NameNotFoundException");
        } catch (NameNotFoundException ex) {
            assertEquals("unable to find a bound object at name 'aaa'", ex.getMessage());
        }

        ctx.close();
    }

}
