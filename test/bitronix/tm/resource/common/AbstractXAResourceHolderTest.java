package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import junit.framework.TestCase;

import javax.transaction.xa.XAResource;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class AbstractXAResourceHolderTest extends TestCase {

    public void testStatesForGtridIterationOrder() throws Exception {
        final ResourceBean resourceBean = new ResourceBean() {
        };

        AbstractXAResourceHolder xaResourceHolder = new AbstractXAResourceHolder() {
            public XAResource getXAResource() {
                return null;
            }

            public ResourceBean getResourceBean() {
                return resourceBean;
            }

            public List getXAResourceHolders() {
                return null;
            }

            public Object getConnectionHandle() throws Exception {
                return null;
            }

            public void close() throws Exception {
            }

            public Date getLastReleaseDate() {
                return null;
            }
        };

        Uid gtrid = UidGenerator.generateUid();

        XAResourceHolderState state1 = new XAResourceHolderState(xaResourceHolder, resourceBean);
        XAResourceHolderState state2 = new XAResourceHolderState(xaResourceHolder, resourceBean);
        XAResourceHolderState state3 = new XAResourceHolderState(xaResourceHolder, resourceBean);

        xaResourceHolder.putXAResourceHolderState(UidGenerator.generateXid(gtrid), state1);
        xaResourceHolder.putXAResourceHolderState(UidGenerator.generateXid(gtrid), state2);
        xaResourceHolder.putXAResourceHolderState(UidGenerator.generateXid(gtrid), state3);


        Map statesForGtrid = xaResourceHolder.getXAResourceHolderStatesForGtrid(gtrid);
        Iterator statesForGtridIt = statesForGtrid.values().iterator();


        assertTrue(statesForGtridIt.hasNext());
        assertSame(state1, statesForGtridIt.next());
        assertTrue(statesForGtridIt.hasNext());
        assertSame(state2, statesForGtridIt.next());
        assertTrue(statesForGtridIt.hasNext());
        assertSame(state3, statesForGtridIt.next());
        assertFalse(statesForGtridIt.hasNext());
    }
}
