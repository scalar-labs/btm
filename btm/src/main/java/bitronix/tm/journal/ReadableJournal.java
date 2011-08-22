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
import java.util.Collection;

/**
 * Gives (unsafe) read access to Journals implementing this interface.
 *
 * @author juergen kellerer, 2011-05-15
 */
public interface ReadableJournal {
    /**
     * Reads all raw journal records and and adds them to the given collection.
     * <p/>
     * <b>Notes:</b><ul>
     * <li>This implementation does not guarantee to return valid results if the journal is in use.
     * The caller is responsible to control this state.</li>
     * <li>The journal is read from the beginning to end with the oldest entry being first. If only
     * a subset of data is required, the given collection should take care to capture the required data.</li>
     * </ul>
     *
     *
     * @param target         the target collection to read the records into.
     * @param includeInvalid specified whether broken records are attempted to be included.
     * @throws java.io.IOException In case of reading the first record fails.
     */
    void unsafeReadRecordsInto(Collection<JournalRecord> target, boolean includeInvalid) throws IOException;
}
