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
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 * @see bitronix.tm.jndi.BitronixInitialContextFactory
 */
public class BitronixContext implements Context {

    private final static Logger log = LoggerFactory.getLogger(BitronixContext.class);
    
    private static final String DEFAULT_USER_TRANSACTION_NAME = "java:comp/UserTransaction";

    private boolean closed = false;
    private String userTransactionName;

    public BitronixContext() {
        userTransactionName = TransactionManagerServices.getConfiguration().getJndiUserTransactionName();
        if (userTransactionName == null)
            userTransactionName = DEFAULT_USER_TRANSACTION_NAME;
        if (log.isDebugEnabled()) log.debug("binding transaction manager at name '" + userTransactionName + "'");
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
        else
            o = ResourceRegistrar.get(s);

        if (o == null)
            throw new NameNotFoundException("unable to find a bound object at name '" + s + "'");
        return o;
    }

    public String toString() {
        return "a BitronixContext with userTransactionName='" + userTransactionName + "'";
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
