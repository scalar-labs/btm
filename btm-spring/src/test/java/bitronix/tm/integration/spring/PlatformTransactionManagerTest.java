package bitronix.tm.integration.spring;

import java.sql.SQLException;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import bitronix.tm.mock.events.EventRecorder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
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

    @Test @Repeat(2) @DirtiesContext
    public void testTransactionalMethod() throws SQLException {
        bean.doSomethingTransactional(1);
        bean.verifyEvents(1);
    }
}
