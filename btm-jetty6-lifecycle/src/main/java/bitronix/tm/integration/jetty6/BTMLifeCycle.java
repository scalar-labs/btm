package bitronix.tm.integration.jetty6;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.log.Log;
import bitronix.tm.TransactionManagerServices;

public class BTMLifeCycle extends AbstractLifeCycle {

    protected void doStart() throws Exception {
        Log.info("Starting Bitronix Transaction Manager");
        TransactionManagerServices.getTransactionManager();
    }

    protected void doStop() throws Exception {
        Log.info("Shutting down Bitronix Transaction Manager");
        TransactionManagerServices.getTransactionManager().shutdown();
    }

}