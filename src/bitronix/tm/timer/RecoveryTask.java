package bitronix.tm.timer;

import bitronix.tm.TransactionManagerServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This task is used to run the background recovery.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class RecoveryTask extends Task {

    private final static Logger log = LoggerFactory.getLogger(RecoveryTask.class);

    public RecoveryTask(Date executionTime) {
        super(executionTime);
    }

    public void execute() throws TaskException {
        if (log.isDebugEnabled()) log.debug("running recovery");
        Thread recovery = new Thread(TransactionManagerServices.getRecoverer());
        recovery.setName("bitronix-recovery-thread");
        recovery.setDaemon(true);
        recovery.setPriority(Thread.NORM_PRIORITY -2);
        recovery.start();

        Date nextExecutionDate = new Date(executionTime.getTime() + (TransactionManagerServices.getConfiguration().getBackgroundRecoveryInterval() * 60L * 1000L));
        if (log.isDebugEnabled()) log.debug("rescheduling recovery for " + nextExecutionDate);
        TransactionManagerServices.getTaskScheduler().scheduleRecovery(nextExecutionDate);
    }

    public String toString() {
        return "a RecoveryTask scheduled for " + executionTime;
    }
    
}
