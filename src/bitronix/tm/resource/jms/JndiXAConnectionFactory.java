package bitronix.tm.resource.jms;

import javax.jms.XAConnectionFactory;
import javax.jms.XAConnection;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.util.Hashtable;
import java.util.Properties;

/**
 * {@link XAConnectionFactory} implementation that wraps another {@link XAConnectionFactory} implementation available
 * in some JNDI tree.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class JndiXAConnectionFactory implements XAConnectionFactory {

    private String initialContextFactory;
    private String providerUrl;
    private String urlPkgPrefixes;
    private String name;
    private String securityPrincipal;
    private String securityCredentials;
    private Properties extraJndiProperties = new Properties();
    private boolean narrowJndiObject = false;
    private XAConnectionFactory wrappedFactory;


    public JndiXAConnectionFactory() {
    }

    /**
     * The {@link Context#INITIAL_CONTEXT_FACTORY} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * @return the {@link Context#INITIAL_CONTEXT_FACTORY} value.
     */
    public String getInitialContextFactory() {
        return initialContextFactory;
    }

    /**
     * Set the {@link Context#INITIAL_CONTEXT_FACTORY} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * If not set, the {@link Context} is created without the environment parameter, using the default constructor. This means
     * <i>all other properties (providerUrl, urlPkgPrefixes, extraJndiProperties...) are then ignored.</i>
     * @param initialContextFactory the {@link Context#INITIAL_CONTEXT_FACTORY} value.
     */
    public void setInitialContextFactory(String initialContextFactory) {
        this.initialContextFactory = initialContextFactory;
    }

    /**
     * The {@link Context#PROVIDER_URL} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * @return the {@link Context#PROVIDER_URL} value.
     */
    public String getProviderUrl() {
        return providerUrl;
    }

    /**
     * Set the {@link Context#PROVIDER_URL} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * @param providerUrl the {@link Context#PROVIDER_URL} value.
     */
    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    /**
     * The {@link Context#URL_PKG_PREFIXES} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * @return the {@link Context#URL_PKG_PREFIXES} value.
     */
    public String getUrlPkgPrefixes() {
        return urlPkgPrefixes;
    }

    /**
     * Set the {@link Context#URL_PKG_PREFIXES} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * @param urlPkgPrefixes the {@link Context#URL_PKG_PREFIXES} value.
     */
    public void setUrlPkgPrefixes(String urlPkgPrefixes) {
        this.urlPkgPrefixes = urlPkgPrefixes;
    }

    /**
     * The {@link Context#SECURITY_PRINCIPAL} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * @return the {@link Context#SECURITY_PRINCIPAL} value.
     */
    public String getSecurityPrincipal() {
        return securityPrincipal;
    }

    /**
     * Set the {@link Context#SECURITY_PRINCIPAL} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * If {@link Context#INITIAL_CONTEXT_FACTORY} and {@link Context#PROVIDER_URL} are not set, this value is ignored.
     * @param securityPrincipal the {@link Context#SECURITY_PRINCIPAL} value.
     */
    public void setSecurityPrincipal(String securityPrincipal) {
        this.securityPrincipal = securityPrincipal;
    }

    /**
     * The {@link Context#SECURITY_CREDENTIALS} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * @return the {@link Context#SECURITY_CREDENTIALS} value.
     */
    public String getSecurityCredentials() {
        return securityCredentials;
    }

    /**
     * Set the {@link Context#SECURITY_CREDENTIALS} of the JNDI {@link Context} used to fetch the {@link XAConnectionFactory}.
     * If {@link Context#INITIAL_CONTEXT_FACTORY} and {@link Context#PROVIDER_URL} are not set, this value is ignored.
     * @param securityCredentials the {@link Context#SECURITY_CREDENTIALS} value.
     */
    public void setSecurityCredentials(String securityCredentials) {
        this.securityCredentials = securityCredentials;
    }

    /**
     * The JNDI name under which the {@link XAConnectionFactory} is available.
     * @return The JNDI name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the JNDI name under which the {@link XAConnectionFactory} is available.
     * @param name the JNDI name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The extra JNDI environment properties added the the {@link InitialContext}'s environment upon creation.
     * @return The extra JNDI environment properties.
     */
    public Properties getExtraJndiProperties() {
        return extraJndiProperties;
    }

    /**
     * Set the extra JNDI environment properties added the the {@link InitialContext}'s environment upon creation.
     * @param extraJndiProperties The extra JNDI environment properties.
     */
    public void setExtraJndiProperties(Properties extraJndiProperties) {
        this.extraJndiProperties = extraJndiProperties;
    }

    /**
     * Should {@link PortableRemoteObject#narrow(Object, Class)} be applied on the object looked up from
     * JNDI before trying to cast it to {@link XAConnectionFactory} ?
     * @return true if the object should be narrowed, false otherwise.
     */
    public boolean isNarrowJndiObject() {
        return narrowJndiObject;
    }

    /**
     * Set if {@link PortableRemoteObject#narrow(Object, Class)} should be applied on the object looked up from
     * JNDI before trying to cast it to {@link XAConnectionFactory} ?
     * @param narrowJndiObject true if the object should be narrowed, false otherwise.
     */
    public void setNarrowJndiObject(boolean narrowJndiObject) {
        this.narrowJndiObject = narrowJndiObject;
    }

    protected void init() throws NamingException {
        if (wrappedFactory != null)
            return;

        Context ctx;
        if (!isEmpty(initialContextFactory)) {
            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactory);
            if (!isEmpty(providerUrl))
                env.put(Context.PROVIDER_URL, providerUrl);
            if (!isEmpty(urlPkgPrefixes))
                env.put(Context.URL_PKG_PREFIXES, urlPkgPrefixes);
            if (!isEmpty(securityPrincipal))
                env.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
            if (!isEmpty(securityCredentials))
                env.put(Context.SECURITY_CREDENTIALS, securityCredentials);
            if (!extraJndiProperties.isEmpty())
                env.putAll(extraJndiProperties);
            ctx = new InitialContext(env);
        }
        else {
            ctx = new InitialContext();
        }

        try {
            Object lookedUpObject = ctx.lookup(name);
            if (narrowJndiObject) {
                wrappedFactory = (XAConnectionFactory) PortableRemoteObject.narrow(lookedUpObject, XAConnectionFactory.class);
            }
            else {
                wrappedFactory = (XAConnectionFactory) lookedUpObject;
            }
        }
        finally {
            ctx.close();
        }
    }

    public XAConnection createXAConnection() throws JMSException {
        try {
            init();
            return wrappedFactory.createXAConnection();
        } catch (NamingException ex) {
            throw (JMSException) new JMSException("error looking up wrapped XAConnectionFactory at " + name).initCause(ex);
        }
    }

    public XAConnection createXAConnection(String userName, String password) throws JMSException {
        try {
            init();
            return wrappedFactory.createXAConnection(userName, password);
        } catch (NamingException ex) {
            throw (JMSException) new JMSException("error looking up wrapped XAConnectionFactory at " + name).initCause(ex);
        }
    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().equals("");
    }

}
