/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
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
package bitronix.tm.utils;

import junit.framework.TestCase;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author lorban
 */
public class UidGeneratorTest extends TestCase {


    public void testHexaStringEncoder() throws Exception {
        byte[] result = Encoder.intToBytes(0x80);
        String hexString = new Uid(result).toString();
        assertEquals("00000080", hexString);

        result = Encoder.longToBytes(0x81);
        hexString = new Uid(result).toString();
        assertEquals("0000000000000081", hexString);

        result = Encoder.shortToBytes((short) 0xff);
        hexString = new Uid(result).toString();
        assertEquals("00FF", hexString);
    }


    public void testUniqueness() throws Exception {
        final int count = 10000;
        HashSet uids = new HashSet(2048);

        for (int i = 0; i < count; i++) {
            Uid uid = UidGenerator.generateUid();
            assertTrue("UidGenerator generated duplicate UID at #" + i, uids.add(uid.toString()));
        }
    }

    public void testMultiThreadedUniqueness() throws Exception {
        final int concurrency = 128, callsPerThread = 1000;
        List<Future<Set<Uid>>> handles = new ArrayList<Future<Set<Uid>>>(concurrency);
        ExecutorService executorService = Executors.newFixedThreadPool(concurrency);
        try {
            for (int i = 0; i < concurrency; i++) {
                handles.add(executorService.submit(new Callable<Set<Uid>>() {
                    public Set<Uid> call() throws Exception {
                        Set<Uid> ids = new HashSet<Uid>(callsPerThread);
                        for (int i = 0; i < callsPerThread; i++)
                            ids.add(UidGenerator.generateUid());
                        return ids;
                    }
                }));
            }
        } finally {
            executorService.shutdown();
        }

        Set<Uid> allIds = new HashSet<Uid>(concurrency * callsPerThread);
        for (Future<Set<Uid>> handle : handles)
            allIds.addAll(handle.get());

        assertEquals(concurrency * callsPerThread, allIds.size());
    }

    public void testEquals() throws Exception {
        Uid uid1 = UidGenerator.generateUid();
        Uid uid2 = UidGenerator.generateUid();
        Uid uid3 = null;

        assertFalse(uid1.equals(uid2));
        assertFalse(uid2.equals(uid3));
        assertTrue(uid2.equals(uid2));
    }

    public void testExtracts() throws Exception {
        byte[] timestamp = Encoder.longToBytes(System.currentTimeMillis());
        byte[] sequence = Encoder.intToBytes(1);
        byte[] serverId = "my-server-id".getBytes();

        int uidLength = serverId.length + timestamp.length + sequence.length;
        byte[] uidArray = new byte[uidLength];

        System.arraycopy(serverId, 0, uidArray, 0, serverId.length);
        System.arraycopy(timestamp, 0, uidArray, serverId.length, timestamp.length);
        System.arraycopy(sequence, 0, uidArray, serverId.length + timestamp.length, sequence.length);

        Uid uid = new Uid(uidArray);

        assertTrue(Arrays.equals(serverId, uid.extractServerId()));
        assertEquals(Encoder.bytesToLong(timestamp, 0), uid.extractTimestamp());
        assertEquals(Encoder.bytesToInt(sequence, 0), uid.extractSequence());
    }

}
