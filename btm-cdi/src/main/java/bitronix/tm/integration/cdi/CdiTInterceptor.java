package bitronix.tm.integration.cdi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


import javax.ejb.ApplicationException;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.RollbackException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

@Interceptor
@CdiTransactional
public class CdiTInterceptor {

    Logger logger = LoggerFactory.getLogger(CdiTInterceptor.class);

    @Inject
    TransactionManager tm;

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Exception {
        final Class<?> declaringClass = ctx.getMethod().getDeclaringClass();
        TFrameStack ts = new TFrameStack();
        TransactionInfo lastTransactionInfo = ts.topTransaction();
        Transactional.TxType attribute = null;
        Transactional transaction =
                declaringClass.getAnnotation(
                        Transactional.class);
        Transactional transactionMethod = ctx.getMethod().getAnnotation(Transactional.class);

        if (transactionMethod != null) {
            attribute = transactionMethod.value();
        } else if (transaction != null) {
            attribute = transaction.value() == null ? Transactional.TxType.REQUIRED : transaction.value();
        }
        if (attribute == null) {
            logger.error("CdiTransactionalInterceptor should not be used at this class: {}", declaringClass.getName());
        } else {
            boolean passThroughRollbackException = true;
            try {
                logger.info("Thread {} L{} changing  from {} to {} xid: {} in {}.{}",
                        Thread.currentThread().getId(), ts.currentLevel(),
                        ts.currentTxType(),
                        attribute, MDC.get("XID"), declaringClass.getSimpleName(), ctx.getMethod().getName());
                ts.pushTransaction(attribute);
            return ctx.proceed();
            } catch (Throwable ex) {
                logger.info("Thread {} L{} Exception {} in {} xid: {} in {}.{}",
                        Thread.currentThread().getId(), ts.currentLevel(),
                        ex.getClass().getSimpleName(), attribute, MDC.get("XID"), declaringClass.getSimpleName(),
                        ctx.getMethod().getName());
                ApplicationException applicationException = null; // TODO
                boolean doRollback =
                        applicationException != null ? applicationException.rollback() : ex instanceof RuntimeException;
                if (doRollback) {
                    passThroughRollbackException = false;
                    tm.rollback();
                }

                if (applicationException == null && ex instanceof RuntimeException) {
                    throw new EJBException((RuntimeException) ex);
                } else {
                    throw ex;
                }
            } finally {
                logger.info("Thread {} L{} finally   in {} xid: {} in {}.{}",
                        Thread.currentThread().getId(), ts.currentLevel(), attribute, MDC.get("XID"), declaringClass.getSimpleName(),
                        ctx.getMethod().getName());
                try {
                    ts.popTransaction();
                } catch (RollbackException rbe) {
                    if (passThroughRollbackException) {
                        throw rbe;
                    }
                } finally {
                    logger.info("Thread {} L{} done      {} back to {} xid: {} in {}.{}",
                            Thread.currentThread().getId(), ts.topTransaction(), attribute,
                            lastTransactionInfo == null ? "undefined" : lastTransactionInfo.currentTxType,
                            MDC.get("XID"), declaringClass.getSimpleName(), ctx.getMethod().getName());
                }
            }
        }
    }

}