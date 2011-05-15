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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.BlockingQueue;
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
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioForceSynchronizer<E> {

    private static final Logger log = LoggerFactory.getLogger(NioForceSynchronizer.class);
    private static final boolean trace = log.isTraceEnabled();

    /**
     * Helper method that unwraps forceable elements.
     *
     * @param source the source collection to read from.
     * @param target the target collection to write the unwrapped content to.
     * @param <E>    the type of the element that is wrapped.
     */
    public static <E> void unwrap(Collection<NioForceSynchronizer<E>.ForceableElement> source, Collection<E> target) {
        if (trace) log.trace("Unwrapping {} sources into target list of size {}", source.size(), target.size());
        for (NioForceSynchronizer<E>.ForceableElement element : source)
            target.add(element.getElement());
    }

    private final ReentrantLock forceLock = new ReentrantLock();
    private final Condition performedForce = forceLock.newCondition();
    private final AtomicLong latestForcedElement = new AtomicLong(),
            latestFailedElement = new AtomicLong(),
            enlistedElementSequence = new AtomicLong();

    private final ThreadLocal<Long> lastEnlistedElementSequenceNumber = new ThreadLocal<Long>();
    private final List<FailedRange> failures = new CopyOnWriteArrayList<FailedRange>();

    /**
     * Enlist the given element inside the specified queue and updates a thread local with the
     * enlisted element's sequence number.
     * <p/>
     * The method waits on queue for space to become available and does not return before enlisting
     * succeeded or the current thread was interrupted.
     *
     * @param element the element to enlist.
     * @param queue   the queue to enlist the element in.
     * @throws InterruptedException in case of the calling thread was interrupted before enlisting succeeded.
     */
    public void enlistElement(E element, BlockingQueue<ForceableElement> queue) throws InterruptedException {
        if (trace) log.trace("Enlisting element {} to get managed in the force synchronizer.", element);

        final ForceableElement wrapper = new ForceableElement(element);
        queue.put(wrapper);
        lastEnlistedElementSequenceNumber.set(wrapper.createOrGetElementNumber());

    }

    /**
     * Wait on the latest, previously enlisted element to get forced or failed.
     *
     * @return returns true if the force operation succeeded and false if an IO error was reported.
     */
    public boolean waitOnEnlisted() {
        Long enlistedElementNumber = lastEnlistedElementSequenceNumber.get();
        if (enlistedElementNumber == null) {
            enlistedElementNumber = enlistedElementSequence.get();
            if (trace) {
                log.trace("The current thread has no enlisted sequence, using the latest sequence {} " +
                        "instead to wait on force.", enlistedElementNumber);
            }
        } else {
            // clearing the sequence, subsequent calls should force everything.
            lastEnlistedElementSequenceNumber.set(null);
        }

        try {
            // Wait until we have our entry forced (may not require a wait at all if it already happened).
            while (enlistedElementNumber > latestForcedElement.get()) {
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
                                             Collection<ForceableElement> elements) throws Exception {
        forceLock.lock();
        try {
            final int waitingThreads = forceLock.getWaitQueueLength(performedForce);
            if (waitingThreads > 0) {
                if (log.isTraceEnabled()) {
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
    public void processEnlisted(Callable forceCommand, Collection<ForceableElement> elements) throws Exception {
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

    private void recordSuccess(Collection<ForceableElement> elements) {
        long largestInList = 0, n;
        for (ForceableElement fe : elements) {
            n = fe.createOrGetElementNumber();
            if (n > largestInList)
                largestInList = n;
        }

        if (incrementTo(latestForcedElement, largestInList))
            if (trace) log.trace("Set the latest forced element sequence to {}.", largestInList);
    }

    private void recordFailures(Collection<ForceableElement> elements) {
        for (ForceableElement element : elements) {
            final long elementNumber = element.createOrGetElementNumber();
            if (incrementTo(latestFailedElement, elementNumber))
                if (trace) log.trace("Set the latest failed element sequence to {}.", elementNumber);

            FailedRange latestRange = failures.isEmpty() ? null : failures.get(failures.size() - 1);
            if (latestRange == null || !latestRange.addToRange(elementNumber)) {
                if (trace) log.trace("Creating new failed range for failed element {}.", element);
                failures.add(new FailedRange(elementNumber));
            }
        }
    }

    private boolean verifyIsInFailedRange(long enlistedElementNumber) {
        if (latestFailedElement.get() >= enlistedElementNumber) {
            for (ListIterator<FailedRange> i = failures.listIterator(failures.size() - 1); i.hasPrevious();) {
                FailedRange failedRange = i.previous();
                if (failedRange.isInRange(enlistedElementNumber)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Reporting that forced failed on entry with sequence number {} ({}).",
                                enlistedElementNumber, failedRange);
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
     * Wraps an element that can be forced.
     */
    public class ForceableElement {

        private final E element;
        private volatile long elementNumber = -1;

        ForceableElement(E element) {
            if (element == null)
                throw new IllegalArgumentException("Element may not be set to 'null'");
            this.element = element;
        }

        final long createOrGetElementNumber() {
            if (elementNumber == -1)
                elementNumber = enlistedElementSequence.incrementAndGet();
            return elementNumber;
        }

        public E getElement() {
            return element;
        }

        @Override
        public String toString() {
            return "ForceableElement{" +
                    "element=" + element +
                    ", elementNumber=" + elementNumber +
                    '}';
        }
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

        boolean isInRange(final long elementNumber) {
            final long lowerBound = firstFailedElement.get(), upperBound = lastFailedElement.get();
            return elementNumber >= lowerBound || elementNumber <= upperBound;
        }

        boolean addToRange(final long elementNumber) {
            final long lowerBound = firstFailedElement.get(), upperBound = lastFailedElement.get();
            if (elementNumber == upperBound + 1)
                lastFailedElement.compareAndSet(upperBound, elementNumber);
            else if (elementNumber == lowerBound - 1)
                firstFailedElement.compareAndSet(lowerBound, elementNumber);

            return isInRange(elementNumber);
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
