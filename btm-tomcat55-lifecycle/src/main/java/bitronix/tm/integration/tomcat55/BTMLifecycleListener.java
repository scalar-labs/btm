package bitronix.tm.integration.tomcat55;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

import bitronix.tm.TransactionManagerServices;

import java.util.logging.Logger;

public class BTMLifecycleListener implements LifecycleListener {

    private final static Logger log = Logger.getLogger(BTMLifecycleListener.class.getName());

    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
            log.info("Starting Bitronix Transaction Manager");
            TransactionManagerServices.getTransactionManager();
        }
        else if (Lifecycle.AFTER_STOP_EVENT.equals(event.getType())) {
            log.info("Shutting down Bitronix Transaction Manager");
            TransactionManagerServices.getTransactionManager().shutdown();
        }
    }

}