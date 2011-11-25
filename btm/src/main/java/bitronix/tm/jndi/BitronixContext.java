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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ResourceRegistrar;

import javax.naming.*;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link javax.naming.Context} that allows lookup of transaction manager
 * and registered resources.
 * <p>This implementation is trivial as only the <code>lookup</code> methods are implemented,
 * all the other ones will throw a {@link OperationNotSupportedException}.</p>
 *
 * @author lorban
 * @see bitronix.tm.jndi.BitronixInitialContextFactory
 */
public class BitronixContext implements Context {

    private final static Logger log = LoggerFactory.getLogger(BitronixContext.class);
    
    private boolean closed = false;
    private final String userTransactionName;
    private final String synchronizationRegistryName;

    public BitronixContext() {
        userTransactionName = TransactionManagerServices.getConfiguration().getJndiUserTransactionName();
        if (log.isDebugEnabled()) log.debug("binding transaction manager at name '" + userTransactionName + "'");

        synchronizationRegistryName = TransactionManagerServices.getConfiguration().getJndiTransactionSynchronizationRegistryName();
        if (log.isDebugEnabled()) log.debug("binding synchronization registry at name '" + synchronizationRegistryName + "'");
    }

    private void checkClosed() throws ServiceUnavailableException {
        if (closed)
            throw new ServiceUnavailableException("context is closed");
    }

    public void close() throws NamingException {
        closed = true;
    }

    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    public Object lookup(String s) throws NamingException {
        checkClosed();
        if (log.isDebugEnabled()) log.debug("looking up '" + s + "'");

        Object o;
        if (userTransactionName.equals(s))
            o = TransactionManagerServices.getTransactionManager();
        else if (synchronizationRegistryName.equals(s))
            o = TransactionManagerServices.getTransactionSynchronizationRegistry();
        else
            o = ResourceRegistrar.get(s);

        if (o == null)
            throw new NameNotFoundException("unable to find a bound object at name '" + s + "'");
        return o;
    }

    public String toString() {
        return "a BitronixContext with userTransactionName='" + userTransactionName + "' and synchronizationRegistryName='" + synchronizationRegistryName + "'";
    }

    public void bind(Name name, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void bind(String s, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rebind(Name name, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rebind(String s, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void unbind(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void unbind(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rename(Name name, Name name1) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rename(String s, String s1) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void destroySubcontext(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Context createSubcontext(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object lookupLink(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object lookupLink(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return BitronixNameParser.INSTANCE;
    }

    public NameParser getNameParser(String s) throws NamingException {
        return BitronixNameParser.INSTANCE;
    }

    public Name composeName(Name name, Name name1) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public String composeName(String s, String s1) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object addToEnvironment(String s, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object removeFromEnvironment(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Hashtable<?, ?> getEnvironment() throws NamingException {
        throw new OperationNotSupportedException();
    }

    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException();
    }

    private final static class BitronixNameParser implements NameParser {
        private static final BitronixNameParser INSTANCE = new BitronixNameParser();

        public Name parse(final String name) throws NamingException {
            return new CompositeName(name);
        }
    }

}
