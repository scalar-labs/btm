package bitronix.tm.resource.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.xa.XAResource;
import java.util.List;
import java.util.Date;

/**
 * {@link XAResourceHolder} created by an {@link bitronix.tm.resource.common.XAResourceProducer} that is
 * used to perform recovery. Objects of this class cannot be used outside recovery scope.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class RecoveryXAResourceHolder extends AbstractXAResourceHolder {

    private final static Logger log = LoggerFactory.getLogger(RecoveryXAResourceHolder.class);

    private XAResourceHolder xaResourceHolder;

    public RecoveryXAResourceHolder(XAResourceHolder xaResourceHolder) {
        this.xaResourceHolder = xaResourceHolder;
    }

    public void close() throws Exception {
        if (log.isDebugEnabled()) log.debug("recovery xa resource is being closed: " + xaResourceHolder);
        xaResourceHolder.setState(STATE_IN_POOL);
    }

    public Date getLastReleaseDate() {
        return null;
    }

    public XAResource getXAResource() {
        return xaResourceHolder.getXAResource();
    }

    public List getXAResourceHolders() {
        return null;
    }

    public Object getConnectionHandle() throws Exception {
        throw new UnsupportedOperationException("illegal connection creation attempt out of " + this);
    }
}
