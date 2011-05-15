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

package bitronix.tm.journal;

import java.io.IOException;

/**
 * May be implemented by journal implementations that support migrating the contained logs into another Journal.
 *
 * @author juergen kellerer, 2011-05-15
 */
public interface MigratableJournal {
    /**
     * Can be called at any point in time to migrate all unfinished transactions into the given
     * journal.
     *
     * @param other the journal to migrate all unfinished transactions to.
     * @throws IOException              In case of not all entries could be written into the other journal.
     * @throws IllegalArgumentException If other is the same instance as 'this'.
     */
    void migrateTo(Journal other) throws IOException, IllegalArgumentException;
}
