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
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

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

    final Queue<ByteBuffer> availableBuffers = new ArrayBlockingQueue<ByteBuffer>(CONCURRENCY);

    {
        for (int i = 0; i < CONCURRENCY; i++)
            availableBuffers.add(ByteBuffer.allocate(PRE_ALLOCATED_BUFFER_SIZE));
    }

    /**
     * Polls a buffer from the pool.
     *
     * @param requiredCapacity the required capacity of the buffer to return.
     * @return a shared buffer instance.
     */
    public ByteBuffer poll(int requiredCapacity) {
        ByteBuffer buffer = availableBuffers.poll();
        return buffer != null && buffer.capacity() >= requiredCapacity ? buffer :
                ByteBuffer.allocate(Math.max(PRE_ALLOCATED_BUFFER_SIZE, requiredCapacity));
    }

    /**
     * Attempt to recycle the given buffer within the pool.
     *
     * @param buffer the buffer to put back into the pool.
     */
    public void recycleBuffer(ByteBuffer buffer) {
        doRecycleBuffer(buffer);
    }

    /**
     * Attempt to recycle the given buffers within the pool.
     *
     * @param buffers the buffers to put back into the pool.
     */
    public void recycleBuffers(Collection<ByteBuffer> buffers) {
        for (ByteBuffer buffer : buffers)
            doRecycleBuffer(buffer);
    }

    private void doRecycleBuffer(ByteBuffer buffer) {
        if (buffer != null && buffer.capacity() == PRE_ALLOCATED_BUFFER_SIZE)
            availableBuffers.offer((ByteBuffer) buffer.clear()); //NOSONAR - return value can be ignored here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NioBufferPool{" +
                "availableBuffers=" + availableBuffers.size() +
                ", CONCURRENCY=" + CONCURRENCY +
                '}';
    }

    private NioBufferPool() {
    }
}
