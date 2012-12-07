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
