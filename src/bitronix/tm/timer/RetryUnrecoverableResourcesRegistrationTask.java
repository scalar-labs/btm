package bitronix.tm.timer;

import bitronix.tm.TransactionManagerServices;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task is used to retry initializing unrecoverable resources registered in the resource loader.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class RetryUnrecoverableResourcesRegistrationTask extends Task {

    private final static Logger log = LoggerFactory.getLogger(RetryUnrecoverableResourcesRegistrationTask.class);

    public RetryUnrecoverableResourcesRegistrationTask(Date executionTime, TaskScheduler scheduler) {
        super(executionTime, scheduler);
    }

    public Object getObject() {
        return "RetryUnrecoverableResourcesRegistrationTask marker";
    }

    public void execute() throws TaskException {
        new Thread() {
            public void run() {
                int errorCount = TransactionManagerServices.getResourceLoader().init();
                if (errorCount > 0) {
                    if (log.isDebugEnabled()) log.debug("some resources still could not recover, rescheduling task");
                    int intervalInMinutes = TransactionManagerServices.getConfiguration().getRetryUnrecoverableResourcesRegistrationInterval();
                    TransactionManagerServices.getTaskScheduler().scheduleRetryUnrecoverableResourcesRegistration(new Date(System.currentTimeMillis() + intervalInMinutes * 60 * 1000));
                }
            }
        }.start();
    }

    public String toString() {
        return "a RetryUnrecoverableResourcesRegistrationTask scheduled for " + getExecutionTime();
    }

}