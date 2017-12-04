package bitronix.tm.integration.cdi;

import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.XAResourceCommitEvent;
import bitronix.tm.mock.events.XAResourceEndEvent;
import bitronix.tm.mock.events.XAResourcePrepareEvent;
import bitronix.tm.mock.events.XAResourceStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.util.Iterator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@EjbTransactional
public class TransactionalJPABean {
    
    private static final Logger log = LoggerFactory.getLogger(TransactionalJPABean.class);

    @Resource(name = "h2DataSource")
    private DataSource dataSource;

    @Inject
    TransactionManager tm;

    @Inject
    EntityManagerFactory entityManagerFactory;

    class CloseableEm implements AutoCloseable {
        private final EntityManager em;

        public CloseableEm(EntityManager em) {
            this.em = em;
        }

        public EntityManager getEm() {
            return em;
        }

        @Override
        public void close() throws Exception {
            em.close();
        }
    }

    CloseableEm getEm() {
        EntityManager result = entityManagerFactory.createEntityManager();
        result.joinTransaction();
        return new CloseableEm(result);
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void insertTestEntityInNewTra() throws Exception {
        Transaction suspendedTransaction = tm.suspend();

        tm.begin();
        try (CloseableEm em = getEm()){
            em.getEm().persist(new TestEntity1());
        } finally {
            tm.commit();
            if (suspendedTransaction != null)
                tm.resume(suspendedTransaction);
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void insertTestEntityInNewTraAndRollback() throws Exception {
        Transaction suspendedTransaction = tm.suspend();

        tm.begin();
        try (CloseableEm em = getEm()){
            em.getEm().persist(new TestEntity1());
        } finally {
            tm.rollback();
            if (suspendedTransaction != null)
                tm.resume(suspendedTransaction);
        }
    }
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void insertTestEntityInNewTraAndSetRollbackOnly() throws Exception {
        Transaction suspendedTransaction = tm.suspend();

        tm.begin();
        try (CloseableEm em = getEm()){
            em.getEm().persist(new TestEntity1());
            tm.setRollbackOnly();
        } finally {
            try {
                tm.commit();

            } catch (RollbackException ex) {

            }

            if (suspendedTransaction != null)
                tm.resume(suspendedTransaction);
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    public void insertTestEntityInRequired() throws Exception {
        boolean encloseInTra = tm.getStatus() == Status.STATUS_NO_TRANSACTION ? true : false;
        if (encloseInTra) {
            tm.begin();
        }
        try (CloseableEm em = getEm()){
            em.getEm().persist(new TestEntity1());
        } finally {
            if (encloseInTra)
                tm.commit();
        }
    }

    public long countTestEntity() throws Exception {
        boolean encloseInTra = tm.getStatus() == Status.STATUS_NO_TRANSACTION ? true : false;
        if (encloseInTra) {
            tm.begin();
        }
        try (CloseableEm em = getEm()) {
            Long result = em.getEm().createQuery("select count(e) from TestEntity1 e", Long.class).getSingleResult();
            return result;
        } finally {
            if (encloseInTra)
                tm.commit();
        }
    }

    public void verifyEvents(int count) {
        if (log.isDebugEnabled()) {
            log.debug(EventRecorder.dumpToString());
        }

        Iterator<?> it = EventRecorder.iterateEvents();

        for (int i = 0; i < count; i++) {
            assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) it.next()).getFlag());
        }
        for (int i = 0; i < count; i++) {
            assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) it.next()).getFlag());
        }
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) it.next()).getReturnCode());
            }
        }
        for (int i = 0; i < count; i++) {
            assertEquals(count == 1, ((XAResourceCommitEvent) it.next()).isOnePhase());
        }

        assertFalse(it.hasNext());
    }
}
