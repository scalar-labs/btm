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

package bitronix.tm.journal;

import bitronix.tm.utils.Uid;

import java.util.Map;
import java.util.Set;

/**
 * Defines the base interface that must be implemented by records that get returned with
 * {@link Journal#collectDanglingRecords()}.
 *
 * @author lorban
 * @author juergen kellerer, 2011-05-01
 */
public interface JournalRecord {
    /**
     * Returns the current status of the transaction that this record belongs to.
     *
     * @return the current status of the transaction that this record belongs to.
     */
    int getStatus();

    /**
     * Returns the global transaction id, identifying the transaction this record belongs to.
     *
     * @return the global transaction id, identifying the transaction this record belongs to.
     */
    Uid getGtrid();

    /**
     * Returns an unmodifiable set of the unique names identifying the components that are part of this transaction.
     *
     * @return an unmodifiable set of the unique names identifying the components that are part of this transaction.
     */
    Set<String> getUniqueNames();

    /**
     * Returns the time when this record was created.
     *
     * @return the time when this record was created.
     */
    long getTime();

    /**
     * Returns true if the record could not only be loaded but does also pass internal checksum verifications.
     *
     * @return true if the record could not only be loaded but does also pass internal checksum verifications.
     */
    boolean isValid();

    /**
     * Returns a map of additional properties that provide access to implementation specific record details.
     *
     * @return a map of additional properties that provide access to implementation specific record details.
     */
    Map<String, ?> getRecordProperties();
}
