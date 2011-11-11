/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import bitronix.tm.utils.CollectionUtils;

/**
 * {@link TransactionLogAppender}s waiting for a disk force get enqueued here.
 *
 * @author lorban
 */
public class DiskForceWaitQueue {

    private final static Logger log = LoggerFactory.getLogger(DiskForceWaitQueue.class);

    private final List<TransactionLogAppender> objects = new ArrayList<TransactionLogAppender>();
    private boolean isCleared = false;


    public DiskForceWaitQueue() {
    }

    /**
     * @return true if the tla was successfully enqueued, false otherwise
     */
    public synchronized boolean enqueue(TransactionLogAppender tla) {
        if (isCleared) {
            return false;
        }
        objects.add(tla);
        if (log.isDebugEnabled()) log.debug("enqueued " + tla + ", " + objects.size() + " TransactionLogAppender waiting for a disk force in " + this);
        notifyAll();
        return true;
    }

    public synchronized TransactionLogAppender head() {
        if (log.isDebugEnabled()) log.debug("returning head TransactionLogAppender");
        return objects.get(0);
    }

    public synchronized void clear() {
        if (log.isDebugEnabled()) log.debug("clearing list of " + objects.size() +  " waiting TransactionLogAppender(s) in " + this);
        objects.clear();
        isCleared = true;
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
