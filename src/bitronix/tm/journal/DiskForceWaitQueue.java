package bitronix.tm.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import bitronix.tm.utils.CollectionUtils;

/**
 * {@link TransactionLogAppender}s waiting for a disk force get enqueued here.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class DiskForceWaitQueue {

    private final static Logger log = LoggerFactory.getLogger(DiskForceWaitQueue.class);

    private List objects = new LinkedList();


    public DiskForceWaitQueue() {
    }

    public synchronized void enqueue(TransactionLogAppender tla) {
        objects.add(tla);
        if (log.isDebugEnabled()) log.debug("enqueued " + tla + ", " + objects.size() + " TransactionLogAppender waiting for a disk force");
        notifyAll();
    }

    public synchronized TransactionLogAppender head() {
        if (log.isDebugEnabled()) log.debug("returning head TransactionLogAppender");
        return (TransactionLogAppender) objects.get(0);
    }

    public synchronized void clear() {
        if (log.isDebugEnabled()) log.debug("clearing list of waiting TransactionLogAppender");
        objects.clear();
        notifyAll();
    }

    public synchronized boolean isEmpty() {
        return objects.isEmpty();
    }

    public synchronized void waitUntilNotEmpty() throws InterruptedException {
        while (objects.isEmpty()) {
            if (log.isDebugEnabled()) log.debug("waiting for some TransactionLogAppender to get enqueued");
            wait();
        }
    }

    public synchronized int size() {
        return objects.size();
    }

    public synchronized void waitUntilNotContains(TransactionLogAppender tla) throws InterruptedException {
        while (CollectionUtils.containsByIdentity(objects, tla)) {
            if (log.isDebugEnabled()) log.debug("waiting for " + tla + " to get dequeued");
            wait();
        }
    }

}
