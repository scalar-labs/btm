/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Juergen Kellerer.
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

package bitronix.tm.journal.nio;

import bitronix.tm.journal.nio.util.SequencedBlockingQueue;
import bitronix.tm.journal.nio.util.SequencedQueueEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Synchronizes 'force' calls between the write requesting threads and the thread that does the actual write IO.
 * <p/>
 * This class is also responsible for transmitting failure cases back to the requester (if it is waiting
 * on force to complete).
 * <p/>
 * Note: This is a low level implementation that is not meant to be used externally.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioForceSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(NioForceSynchronizer.class);
    private static final boolean trace = log.isTraceEnabled();

    private final ReentrantLock forceLock = new ReentrantLock();
    private final Condition performedForce = forceLock.newCondition();
    private final AtomicLong latestForcedElement = new AtomicLong(), latestFailedElement = new AtomicLong();

    private final List<FailedRange> failures = new CopyOnWriteArrayList<FailedRange>();

    private final SequencedBlockingQueue pendingRecordsQueue;

    NioForceSynchronizer(SequencedBlockingQueue pendingRecordsQueue) {
        this.pendingRecordsQueue = pendingRecordsQueue;
    }

    /**
     * Wait on the latest, previously enlisted element to get forced or failed.
     * <p/>
     * Important: A call to this method measures the storage of all enlisted element that
     * were enlisted in the current thread under the assumption that elements are stored
     * in the order they were placed in the queue.
     *
     * @return returns true if the force operation succeeded and false if an IO error was reported.
     */
    public boolean waitOnEnlisted() {
        long enlistedElementNumber = pendingRecordsQueue.getMaxElementSequenceNumberForCurrentThread(true);

        try {
            // Wait until we have our entry forced (may not require a wait at all if it already happened).
            while (enlistedElementNumber > latestForcedElement.get()) {
                if (verifyIsInFailedRange(enlistedElementNumber))
                    return false;

                forceLock.lockInterruptibly();
                try {
                    if (trace) log.trace("Waiting until entry with sequence {} was forced.", enlistedElementNumber);
                    performedForce.await();
                } finally {
                    forceLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        // Check if we had an exception.
        if (verifyIsInFailedRange(enlistedElementNumber)) {
            return false;
        } else {
            if (trace) {
                log.trace("Entry with sequence {} was successfully forced (force ranges up to {}).",
                        enlistedElementNumber, latestForcedElement.get());
            }
            return true;
        }
    }

    /**
     * Processes the enlisted elements with the given force command and notifies the waiting
     * threads on success or failure. The operation does nothing if no thread is waiting.
     *
     * @param forceCommand the command to run prior to notifying the threads.
     * @param elements     the elements that are processed by command (and are the base for notification).
     * @return returns true if the command was called and threads have been notified.
     * @throws Exception the exception thrown by the command if it failed.
     */
    public boolean processEnlistedIfRequired(Callable forceCommand,
                                             Collection<? extends SequencedQueueEntry> elements)
            throws Exception {
        forceLock.lock();
        try {
            final int waitingThreads = forceLock.getWaitQueueLength(performedForce);
            if (waitingThreads > 0) {
                if (trace) {
                    log.trace("Found {} threads waiting on force to happen. Forcing {} " +
                            "log entries to disk now.", waitingThreads, elements);
                }

                processEnlisted(forceCommand, elements);
                return true;
            }
        } finally {
            forceLock.unlock();
        }
        return false;
    }

    /**
     * Processes the enlisted elements with the given force command and notifies any waiting
     * threads on success or failure.
     *
     * @param forceCommand the command to run prior to notifying the threads.
     * @param elements     the elements that are processed by command (and are the base for notification).
     * @throws Exception the exception thrown by the command if it failed.
     */
    public void processEnlisted(Callable forceCommand,
                                Collection<? extends SequencedQueueEntry> elements) throws Exception {
        forceLock.lock();
        try {
            try {
                forceCommand.call();
                recordSuccess(elements);
            } catch (Exception e) {
                recordFailures(elements);
                throw e;
            } finally {
                performedForce.signalAll();
            }
        } finally {
            forceLock.unlock();
        }
    }

    private void recordSuccess(Collection<? extends SequencedQueueEntry> elements) {
        long largestInList = 0, n;
        for (SequencedQueueEntry fe : elements) {
            n = fe.getSequenceNumber();
            if (n > largestInList)
                largestInList = n;
        }

        if (incrementTo(latestForcedElement, largestInList))
            if (trace) log.trace("Set the latest forced element sequence to {}.", largestInList);
    }

    private void recordFailures(Collection<? extends SequencedQueueEntry> elements) {
        for (SequencedQueueEntry element : elements) {
            final long elementNumber = element.getSequenceNumber();
            if (incrementTo(latestFailedElement, elementNumber))
                if (trace) log.trace("Set the latest failed element sequence to {}.", elementNumber);

            FailedRange latestRange = failures.isEmpty() ? null : failures.get(failures.size() - 1);
            if (latestRange == null || !latestRange.addToRange(elementNumber)) {
                if (trace) log.trace("Creating new failed range for failed element {}.", element);
                failures.add(new FailedRange(elementNumber));
            }
        }
    }

    private boolean verifyIsInFailedRange(long elementSequenceNumber) {
        if (latestFailedElement.get() >= elementSequenceNumber) {
            final boolean debug = log.isDebugEnabled();
            for (ListIterator<FailedRange> i = failures.listIterator(failures.size()); i.hasPrevious(); ) {
                FailedRange failedRange = i.previous();
                if (failedRange.isInRange(elementSequenceNumber)) {
                    if (debug) {
                        log.debug("Reporting that force failed on entry with sequence number {} ({}).",
                                elementSequenceNumber, failedRange);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean incrementTo(final AtomicLong number, final long toNumber) {
        long n;
        while ((n = number.get()) <= toNumber) {
            if (number.compareAndSet(n, toNumber))
                return true;
        }
        return false;
    }

    /**
     * Keeps a range of failed elements.
     */
    static class FailedRange {

        final long createTime = System.currentTimeMillis();
        final AtomicLong firstFailedElement, lastFailedElement;

        FailedRange(long initialNumber) {
            this.firstFailedElement = new AtomicLong(initialNumber);
            this.lastFailedElement = new AtomicLong(initialNumber);
        }

        boolean isInRange(final long elementSequenceNumber) {
            final long lowerBound = firstFailedElement.get(), upperBound = lastFailedElement.get();
            return elementSequenceNumber >= lowerBound && elementSequenceNumber <= upperBound;
        }

        boolean addToRange(final long elementSequenceNumber) {
            final long lowerBound = firstFailedElement.get(), upperBound = lastFailedElement.get();
            if (elementSequenceNumber == upperBound + 1)
                lastFailedElement.compareAndSet(upperBound, elementSequenceNumber);
            else if (elementSequenceNumber == lowerBound - 1)
                firstFailedElement.compareAndSet(lowerBound, elementSequenceNumber);

            return isInRange(elementSequenceNumber);
        }

        @Override
        public String toString() {
            return "FailedRange{" +
                    "createTime=" + new Date(createTime) +
                    ", firstFailedElement=" + firstFailedElement +
                    ", lastFailedElement=" + lastFailedElement +
                    '}';
        }
    }
}
