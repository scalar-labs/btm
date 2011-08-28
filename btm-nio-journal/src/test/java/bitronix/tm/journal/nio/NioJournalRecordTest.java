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
 * Smoke test on the NioJournalRecord.
 *
 * @author juergen kellerer, 2011-04-30
 */
public class NioJournalRecordTest {
    @Test
    public void testWriteToBuffer() throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(1024);

        for (int i = 0; i < 10; i++) {
            Uid gtrid = UidGenerator.generateUid();
            Set<String> names = new TreeSet<String>(Arrays.asList("a", "", "another-name", "äöü"));
            NioJournalRecord lr = new NioJournalRecord(1, gtrid, names);

            lr.encodeTo((ByteBuffer) bb.clear(), false);
            NioJournalRecord decodedLr = new NioJournalRecord((ByteBuffer) bb.flip(), true);

            assertEquals(gtrid, decodedLr.getGtrid());
            assertEquals(names, decodedLr.getUniqueNames());
            assertEquals(lr, decodedLr);

            assertEquals(lr.getRecordLength(), bb.position());
        }
    }
}
