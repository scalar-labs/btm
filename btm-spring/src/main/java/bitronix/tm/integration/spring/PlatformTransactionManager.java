package bitronix.tm.integration.spring;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.jta.JtaTransactionManager;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;

/**
 * Bitronix-specific Spring PlatformTransactionManager implementation.
 * 
 * @author Marcus Klimstra (CGI)
 */
public class PlatformTransactionManager extends JtaTransactionManager implements DisposableBean {

    private final BitronixTransactionManager transactionManager;;

    public PlatformTransactionManager() {
        this.transactionManager = TransactionManagerServices.getTransactionManager();
    }

    @Override
    protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
        return transactionManager;
    }

    @Override
    protected TransactionManager retrieveTransactionManager() throws TransactionSystemException {
        return transactionManager;
    }

    @Override
    protected Object retrieveTransactionSynchronizationRegistry() throws TransactionSystemException {
        return transactionManager;
    }

    public void destroy() throws Exception {
        transactionManager.shutdown();
    }
}
