/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.journal;

import bitronix.tm.utils.Uid;

import java.util.Map;
import java.util.Set;

/**
 * Defines the base interface that must be implemented by records that get returned with
 * {@link Journal#collectDanglingRecords()}.
 *
 * @author Ludovic Orban
 * @author Juergen Kellerer
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
