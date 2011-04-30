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

import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

/**
 * Smoke test on the NioLogRecord.
 *
 * @author juergen kellerer, 2011-04-30
 */
public class NioLogRecordTest {
    @Test
    public void testWriteToBuffer() throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(1024);

        for (int i = 0; i < 10; i++) {
            Uid gtrid = UidGenerator.generateUid();
            Set<String> names = new TreeSet<String>(Arrays.asList("a", "", "another-name", "äöü"));
            NioLogRecord lr = new NioLogRecord(1, gtrid, names);

            lr.encodeTo((ByteBuffer) bb.clear());
            NioLogRecord decodedLr = new NioLogRecord(bb);

            assertEquals(lr.getCrc32(), decodedLr.getCrc32());
            assertEquals(gtrid, decodedLr.getGtrid());
            assertEquals(names, decodedLr.getUniqueNames());

            assertEquals(lr.getEffectiveRecordLength(), bb.position());
        }
    }
}
