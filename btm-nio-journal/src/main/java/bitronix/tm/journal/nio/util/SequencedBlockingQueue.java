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

package bitronix.tm.journal.nio.util;

import bitronix.tm.journal.nio.NioJournalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements a bounded blocking queue that maintains a sequence number with all added elements.
 * <p/>
 * The sequence number is created when putting elements to the queue and is based on an ever increasing
 * element sequence.
 * <p/>
 * This queue maintains the maximum sequence number of the latest addition in a ThreadLocal, allowing
 * multiple threads to have different maximum sequence numbers. This feature is used to determine the sequence
 * of the latest addition to this queue when a thread needs to wait on elements being processed outside
 * of the queue.
 *
 * @author Juergen_Kellerer, 2011-08-23
 * @version 1.0
 */
public final class SequencedBlockingQueue<E>
        extends ArrayBlockingQueue<SequencedQueueEntry<E>>
        implements NioJournalConstants {

    private static final Logger log = LoggerFactory.getLogger(SequencedBlockingQueue.class);
    private static final boolean trace = log.isTraceEnabled();

    /**
     * Helper method that unwraps entries to their wrapped elements.
     *
     * @param source the source collection to read from.
     * @param target the target collection to write the unwrapped content to.
     * @param <E>    the type of the element that is wrapped.
     */
    public static <E> void unwrap(Collection<SequencedQueueEntry<E>> source, Collection<? super E> target) {
        if (trace) log.trace("Unwrapping {} sources into target list of size {}", source.size(), target.size());
        for (SequencedQueueEntry<E> entry : source) target.add(entry.getElement());
    }

    private final ReentrantLock lock;
    private final AtomicLong enlistedElementSequence = new AtomicLong();
    private final ThreadLocal<Long> lastEnlistedElementSequenceNumber = new ThreadLocal<Long>();

    /**
     * Creates a instance of SequencedBlockingQueue with a capacity of {@link #CONCURRENCY} lock-free puts.
     */
    public SequencedBlockingQueue() {
        super(CONCURRENCY);

        ReentrantLock lock;
        try {
            Field lockField = getClass().getSuperclass().getDeclaredField("lock");
            lockField.setAccessible(true);
            lock = (ReentrantLock) lockField.get(this);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            log.info("Failed to access shared ReentrantLock instance. " +
                    "Will need to acquire a separate lock when putting elements into this queue.", e);
            lock = new ReentrantLock(false);
        }

        this.lock = lock;
    }

    /**
     * Enlist the given element inside the specified queue and updates a thread local with the
     * enlisted element's sequence number.
     * <p/>
     * The method waits for space to become available and does not return before enlisting
     * succeeded or the current thread was interrupted.
     *
     * @param element the element to enlist.
     * @return the sequence number that was assigned with the enlisted element.
     * @throws InterruptedException in case of the calling thread was interrupted before enlisting succeeded.
     */
    public long putElement(E element) throws InterruptedException {
        if (trace) log.trace("Putting the element {} to the queue of pending records.", element);

        final SequencedQueueEntry<E> entry = new SequencedQueueEntry<E>(element);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            put(entry);

            // Note: The sequence number is created when the entry was successfully put into the queue.
            //       This avoids that getMaxElementNumber() can return a number of a non-enlisted entry.

            final long sequenceNumber = enlistedElementSequence.incrementAndGet();
            entry.setSequenceNumber(sequenceNumber);
            lastEnlistedElementSequenceNumber.set(sequenceNumber);

            return sequenceNumber;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the maximum element sequence number added by any thread.
     *
     * @return the maximum element sequence number added by any thread.
     */
    public long getMaxElementSequenceNumber() {
        return enlistedElementSequence.get();
    }

    /**
     * Returns the maximum element sequence number of the element added by the calling thread.
     * <p/>
     * If the thread did not add any elements, the method returns the same result as
     * {@link #getMaxElementSequenceNumber()}.
     *
     * @param clearThreadLocal if true, clears the thread local sequence number to ensure subsequent
     *                         calls will return the maximum number instead.
     * @return the maximum element sequence number of the element added by the calling thread.
     */
    public long getMaxElementSequenceNumberForCurrentThread(boolean clearThreadLocal) {
        Long maxElementSequence = lastEnlistedElementSequenceNumber.get();
        if (maxElementSequence == null) {
            maxElementSequence = getMaxElementSequenceNumber();
            if (trace) {
                log.trace("The current thread has no enlisted sequence, using the latest sequence {} " +
                        "instead to wait on force.", maxElementSequence);
            }
        } else if (clearThreadLocal) {
            // clearing the sequence, subsequent calls should force everything.
            lastEnlistedElementSequenceNumber.set(null);
        }

        return maxElementSequence;
    }

    /**
     * Drains all queued elements to the target collection.
     *
     * @param target the target collection to add the queued elements to.
     * @return the number of elements added to target.
     */
    public int drainElementsTo(Collection<? super E> target) {
        return drainElementsTo(new LinkedList<SequencedQueueEntry<E>>(), target);
    }

    /**
     * Drains all queued elements to the target collection.
     *
     * @param entries a collection that takes the entries containing the elements.
     * @param target  the target collection to add the queued elements to.
     * @return the number of elements added to target.
     */
    public int drainElementsTo(List<SequencedQueueEntry<E>> entries, Collection<? super E> target) {
        try {
            return transferElements(entries, target, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Takes one entry from this queue and drains any further queued elements to the target collection.
     * <p/>
     * This method blocks until at least one element was added.
     *
     * @param target the target collection to add the queued elements to.
     * @return the number of elements added to target.
     * @throws InterruptedException In case of the calling thread was interrupted.
     */
    public int takeAndDrainElementsTo(Collection<? super E> target) throws InterruptedException {
        return takeAndDrainElementsTo(new LinkedList<SequencedQueueEntry<E>>(), target);
    }

    /**
     * Takes one entry from this queue and drains any further queued elements to the target collection.
     * <p/>
     * This method blocks until at least one element was added.
     *
     * @param entries a collection that takes the entries containing the elements.
     * @param target  the target collection to add the queued elements to.
     * @return the number of elements added to target.
     * @throws InterruptedException In case of the calling thread was interrupted.
     */
    public int takeAndDrainElementsTo(List<SequencedQueueEntry<E>> entries,
                                      Collection<? super E> target) throws InterruptedException {
        return transferElements(entries, target, true);
    }

    private int transferElements(final List<SequencedQueueEntry<E>> entries,
                                 final Collection<? super E> elements,
                                 final boolean block) throws InterruptedException {
        int transferCount = 0;
        final int startIndex = entries.size();

        if (block) {
            entries.add(take());
            transferCount++;
        }

        transferCount += drainTo(entries);

        if (startIndex == 0)
            unwrap(entries, elements);
        else
            unwrap(entries.subList(startIndex, entries.size()), elements);

        return transferCount;
    }
}
