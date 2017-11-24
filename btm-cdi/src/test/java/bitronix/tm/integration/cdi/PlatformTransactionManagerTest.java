package bitronix.tm.integration.cdi;

import java.sql.SQLException;

import javax.inject.Inject;

import com.oneandone.ejbcdiunit.EjbUnitRunner;
import org.jglue.cdiunit.ActivatedAlternatives;
import org.jglue.cdiunit.AdditionalClasses;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.mock.events.EventRecorder;

@RunWith(EjbUnitRunner.class)
@AdditionalClasses({PlatformTransactionManager.class})
@ActivatedAlternatives({DataSource1.class})
public class PlatformTransactionManagerTest {

    private static final Logger log = LoggerFactory.getLogger(PlatformTransactionManagerTest.class);

    @Inject
    private TransactionalBean bean;

    @Before @After
    public void clearEvents() {
        EventRecorder.clear();
    }

    @After
    public void logEvents() {
        if (log.isDebugEnabled()) {
            log.debug(EventRecorder.dumpToString());
        }
    }

    @Test
    public void testTransactionalMethod() throws Exception {
        bean.doSomethingTransactional(1);
        bean.verifyEvents(1);
    }
}
