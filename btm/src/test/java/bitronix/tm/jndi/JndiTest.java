/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.jndi;

import java.util.Hashtable;

import javax.naming.*;

import junit.framework.TestCase;
import bitronix.tm.*;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.jdbc.PoolingDataSource;


/**
 *
 * @author lorban
 */
public class JndiTest extends TestCase {

    private BitronixTransactionManager transactionManager;

    protected void setUp() throws Exception {
        transactionManager = TransactionManagerServices.getTransactionManager();
    }

    protected void tearDown() throws Exception {
        transactionManager.shutdown();
    }

    public void testNameParser() throws Exception {
        BitronixContext bitronixContext = new BitronixContext();
        Name name = bitronixContext.getNameParser("").parse("java:comp/UserTransaction");
        assertEquals("java:comp/UserTransaction", name.toString());
        assertSame(BitronixTransactionManager.class, bitronixContext.lookup(name).getClass());

        name = bitronixContext.getNameParser(new CompositeName()).parse("java:comp/UserTransaction");
        assertEquals("java:comp/UserTransaction", name.toString());
        assertSame(BitronixTransactionManager.class, bitronixContext.lookup(name).getClass());
    }

    public void testDefaultUserTransactionAndResources() throws Exception {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setMaxPoolSize(1);
        pds.setClassName(MockitoXADataSource.class.getName());
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
