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

/**
 * Wraps an element that is put into the SequencedBlockingQueue.
 * <p/>
 * The purpose of this wrapper is to have the ability to assign sequence
 * numbers with queue entries that can be assigned lazily.
 */
public final class SequencedQueueEntry<E> {

    private final E element;
    private volatile long sequenceNumber = -1;

    /**
     * Constructs a new entry for the SequencedBlockingQueue.
     *
     * @param element the payload to wrap in this instance.
     */
    SequencedQueueEntry(E element) {
        if (element == null)
            throw new IllegalArgumentException("Element may not be set to 'null'");
        this.element = element;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public E getElement() {
        return element;
    }

    @Override
    public String toString() {
        return "SequencedQueueEntry{" +
                "element=" + element +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}
