/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package bitronix.tm.journal.nio;

import bitronix.tm.journal.Journal;
import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.Status;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Nio journal specific functional tests.
 *
 * @author juergen kellerer, 2011-04-30
 */
public class NioJournalFunctionalTest extends AbstractJournalFunctionalTest {
    @Override
    protected TransactionLogRecord getLogRecord(int status, int recordLength, int headerLength,
                                                long time, int sequenceNumber, int crc32, Uid gtrid,
                                                Set uniqueNames, int endRecord) {
        return new NioLogRecord(status, recordLength, headerLength, time,
                sequenceNumber, crc32, gtrid, uniqueNames, endRecord);
    }

    @Override
    protected Journal getJournal() {
        return new NioJournal();
    }

    @Before
    public void setUp() throws Exception {
        NioJournal.getJournalFilePath().delete();
    }

    @Test
    public void testJournalIsPositionedCorrectlyAfterOpen() throws Exception {
        int[] tests = {1, 2, 233, 4493};
        for (int test : tests) {
            setUp();
            doTestPositionIsCorrect(test);
            shutdownJournal();
        }
    }

    private void doTestPositionIsCorrect(int iterations) throws IOException {
        Uid gtrid = UidGenerator.generateUid();
        HashSet<String> uniqueNames = new HashSet<String>(Arrays.asList("1"));
        int rawRecordSize = NioJournalFileRecord.RECORD_HEADER_SIZE + NioJournalFileRecord.RECORD_TRAILER_SIZE +
                new NioLogRecord(Status.STATUS_ACTIVE, gtrid, uniqueNames).getEffectiveRecordLength();

        journal.open();
        for (int i = 0; i < iterations; i++)
            journal.log(Status.STATUS_ACTIVE, gtrid, uniqueNames);

        journal.close();
        assertFalse(((NioJournal) journal).isOpen());

        journal.open();
        assertEquals("Iterations:" + iterations,
                NioJournalFile.FIXED_HEADER_SIZE + (iterations * rawRecordSize),
                ((NioJournal) journal).journalFile.getPosition());
    }
}
