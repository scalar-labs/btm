package bitronix.tm.timer;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.recovery.Recoverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This task is used to run the background recovery.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class RecoveryTask extends Task {

    private final static Logger log = LoggerFactory.getLogger(RecoveryTask.class);

    private Recoverer recoverer;

    public RecoveryTask(Recoverer recoverer, Date executionTime, TaskScheduler scheduler) {
        super(executionTime, scheduler);
        this.recoverer = recoverer;
    }

    public Object getObject() {
        return recoverer;
    }

    public void execute() throws TaskException {
        if (log.isDebugEnabled()) log.debug("running recovery");
        Thread recovery = new Thread(recoverer);
        recovery.setName("bitronix-recovery-thread");
        recovery.setDaemon(true);
        recovery.setPriority(Thread.NORM_PRIORITY -1);
        recovery.start();

        Date nextExecutionDate = new Date(getExecutionTime().getTime() + (TransactionManagerServices.getConfiguration().getBackgroundRecoveryIntervalSeconds() * 1000L));
        if (log.isDebugEnabled()) log.debug("rescheduling recovery for " + nextExecutionDate);
        getTaskScheduler().scheduleRecovery(recoverer, nextExecutionDate);
    }

    public String toString() {
        return "a RecoveryTask scheduled for " + getExecutionTime();
    }
    
}
