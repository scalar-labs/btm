package bitronix.tm.integration.jetty7;

import bitronix.tm.TransactionManagerServices;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;

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
