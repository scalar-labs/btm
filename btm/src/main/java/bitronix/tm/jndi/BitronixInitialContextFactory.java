/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.jndi;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
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
 * @author Ludovic Orban
 * @see bitronix.tm.jndi.BitronixContext
 */
public class BitronixInitialContextFactory implements InitialContextFactory {

    @Override
    public Context getInitialContext(Hashtable<?,?> hashtable) throws NamingException {
        return new BitronixContext();
    }

    @Override
    public String toString() {
        return "a BitronixInitialContextFactory";
    }

}
