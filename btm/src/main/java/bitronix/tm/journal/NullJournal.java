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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * No-op journal. Do not use for anything else than testing as the transaction manager cannot guarantee
 * data integrity with this journal implementation.
 *
 * @author lorban
 */
public class NullJournal implements Journal {

    public NullJournal() {
    }

    public void log(int status, Uid gtrid, Set<String> uniqueNames) throws IOException {
    }

    public void open() throws IOException {
    }

    public void close() throws IOException {
    }

    public void force() throws IOException {
    }

    public Map<Uid, JournalRecord> collectDanglingRecords() throws IOException {
        return Collections.emptyMap();
    }

    public void shutdown() {
    }

    public String toString() {
        return "a NullJournal";
    }
}
