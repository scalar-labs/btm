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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.TransactionLogAppender;
import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.utils.Uid;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.Status;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertTrue;

/**
 * Classic journal specific functional tests.
 *
 * @author lorban
 */
public class DiskJournalFunctionalTest extends AbstractJournalFunctionalTest {
    @Override
    protected TransactionLogRecord getLogRecord(int status, int recordLength, int headerLength,
                                                long time, int sequenceNumber, int crc32, Uid gtrid,
                                                Set uniqueNames, int endRecord) {
        return new TransactionLogRecord(status, recordLength, headerLength, time, sequenceNumber,
                crc32, gtrid, uniqueNames, endRecord);
    }

    @Override
    protected Journal getJournal() {
        return new DiskJournal();
    }

    @Before
    public void setUp() throws Exception {
        new File(TransactionManagerServices.getConfiguration().getLogPart1Filename()).delete();
        new File(TransactionManagerServices.getConfiguration().getLogPart2Filename()).delete();
    }

    @Test
    public void testCrc32Value() throws Exception {
        Set names = new HashSet();
        names.add("ActiveMQ");
        names.add("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");

        String uidString = "626974726F6E697853657276657249440000011C31FD45510000955B";
        byte[] uidArray = new byte[uidString.length() / 2];
        for (int i = 0; i < uidString.length() / 2; i++) {
            String substr = uidString.substring(i * 2, i * 2 + 2);
            byte b = (byte) Integer.parseInt(substr, 16);

            uidArray[i] = b;
        }
        Uid uid = new Uid(uidArray);

        TransactionLogRecord tlr = getLogRecord(Status.STATUS_COMMITTED,
                116, 28, 1220609394845L, 38266, -1380478121, uid, names, TransactionLogAppender.END_RECORD);
        assertTrue(tlr.isCrc32Correct());

        names = new TreeSet();
        names.add("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
        names.add("ActiveMQ");

        tlr = getLogRecord(Status.STATUS_COMMITTED,
                116, 28, 1220609394845L, 38266, -1380478121, uid, names, TransactionLogAppender.END_RECORD);
        assertTrue(tlr.isCrc32Correct());

        // test that removing unique names refreshes CRC32, see BTM-44
        Set namesToRemove = new HashSet();
        namesToRemove.add("ActiveMQ");

        tlr.removeUniqueNames(namesToRemove);

        assertTrue(tlr.isCrc32Correct());
    }
}
