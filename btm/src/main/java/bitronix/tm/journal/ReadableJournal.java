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
