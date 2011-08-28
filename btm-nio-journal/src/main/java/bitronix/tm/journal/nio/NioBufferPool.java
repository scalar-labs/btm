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

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * Simple pool of pre-allocated byte buffers used for concurrent serialization.
 *
 * @author juergen kellerer, 2011-04-30
 */
final class NioBufferPool implements NioJournalConstants {

    private static final NioBufferPool instance = new NioBufferPool();

    public static NioBufferPool getInstance() {
        return instance;
    }

    /**
     * Polls a buffer from the pool.
     *
     * @param requiredCapacity the required capacity of the buffer to return.
     * @return a shared buffer instance.
     */
    public ByteBuffer poll(int requiredCapacity) {
        // Pooling is disabled for now as it seems to be cheaper to create a
        // fresh buffer instead of using any sort of concurrent queue (for capacities of ~200 bytes per average).
        return ByteBuffer.allocate(requiredCapacity);
    }

    private void put(ByteBuffer buffer) {
    }

    /**
     * Attempt to recycle the given buffer within the pool.
     *
     * @param buffer the buffer to put back into the pool.
     */
    public void recycleBuffer(ByteBuffer buffer) {
        put(buffer);
    }

    /**
     * Attempt to recycle the given buffers within the pool.
     *
     * @param buffers the buffers to put back into the pool.
     */
    public void recycleBuffers(Collection<ByteBuffer> buffers) {
        for (ByteBuffer buffer : buffers)
            put(buffer);
    }

    private NioBufferPool() {
    }
}
