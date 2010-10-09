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

import javax.naming.spi.InitialContextFactory;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Implementation of {@link InitialContextFactory} that allows lookup of transaction manager
 * and registered resources.
 * <p>
 * The easiest way to use this provider is to create a <code>jndi.properties</code> file
 * in your classpath with this content:
 * <pre>java.naming.factory.initial=bitronix.tm.jndi.BitronixInitialContextFactory</pre>
 * Alternatively, you can create a {@link javax.naming.InitialContext} object with an environment
 * pointing to this class:
 * <pre>
 * Hashtable env = new Hashtable();
 * env.put(Context.INITIAL_CONTEXT_FACTORY, "bitronix.tm.jndi.BitronixInitialContextFactory");
 * Context ctx = new InitialContext(env);
 * </pre>
 * </p>
 * <p>The transaction manager can be looked up at the standard URL <code>java:comp/UserTransaction</code>
 * while resources can be looked up using their unique name as set in 
 * {@link bitronix.tm.resource.common.ResourceBean#getUniqueName()}.
 * </p>
 *
 * @author lorban
 * @see bitronix.tm.jndi.BitronixContext
 */
public class BitronixInitialContextFactory implements InitialContextFactory {

    public Context getInitialContext(Hashtable hashtable) throws NamingException {
        return new BitronixContext();
    }

    public String toString() {
        return "a BitronixInitialContextFactory";
    }

}
