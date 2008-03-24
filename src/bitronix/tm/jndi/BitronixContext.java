package bitronix.tm.jndi;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ResourceRegistrar;

import javax.naming.*;
import java.util.Hashtable;

/**
 * Implementation of {@link javax.naming.Context} that allows lookup of transaction manager
 * and registered resources.
 * <p>This implementation is tivial as only the <code>lookup</code> methods are implemented,
 * all the other ones will throw a {@link OperationNotSupportedException}.</p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 * @see bitronix.tm.jndi.BitronixInitialContextFactory
 */
public class BitronixContext implements Context {

    private static final String USER_TRANSACTION_NAME = "java:comp/UserTransaction";

    private boolean closed = false;

    public BitronixContext() {
    }

    private void checkClosed() throws ServiceUnavailableException {
        if (closed)
            throw new ServiceUnavailableException("context is closed");
    }

    public void close() throws NamingException {
        closed = true;
    }

    public Object lookup(Name name) throws NamingException {
        checkClosed();
        return lookup(name.toString());
    }

    public Object lookup(String s) throws NamingException {
        if (USER_TRANSACTION_NAME.equals(s))
            return TransactionManagerServices.getTransactionManager();
        return ResourceRegistrar.get(s);
    }

    public String toString() {
        return "a BitronixContext";
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

    public NamingEnumeration list(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration list(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration listBindings(String s) throws NamingException {
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
        throw new OperationNotSupportedException();
    }

    public NameParser getNameParser(String s) throws NamingException {
        throw new OperationNotSupportedException();
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

    public Hashtable getEnvironment() throws NamingException {
        throw new OperationNotSupportedException();
    }

    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException();
    }
}
