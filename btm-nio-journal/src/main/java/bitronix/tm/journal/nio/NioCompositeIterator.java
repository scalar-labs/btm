/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Bitronix Software.
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

import java.util.Iterator;

/**
 * Utility class that creates a composite of one or more iterators.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioCompositeIterator implements Iterator<NioJournalFileRecord> {

    final Iterator<Iterable<NioJournalFileRecord>> iterators;
    Iterator<NioJournalFileRecord> current;

    /**
     * Creates an instance out of the given list of iterables.
     *
     * @param sources the source iterables to use for creating the iterator.
     */
    public NioCompositeIterator(Iterable<Iterable<NioJournalFileRecord>> sources) {
        iterators = sources.iterator();
        current = iterators.hasNext() ? iterators.next().iterator() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        if (current == null)
            return false;
        if (!current.hasNext()) {
            while (iterators.hasNext()) {
                current = iterators.next().iterator();
                if (current.hasNext())
                    return true;
            }
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NioJournalFileRecord next() {
        return current.next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        current.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NioCompositeIterator{" +
                "current=" + current +
                '}';
    }
}
