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
package bitronix.tm.twopc.executor;

import bitronix.tm.internal.BitronixRuntimeException;

/**
 * This implementation spawns a new thread per request.
 *
 * @author lorban
 */
public class SimpleAsyncExecutor implements Executor {

    public Object submit(Job job) {
        Thread t = new Thread(job);
        t.setDaemon(true);
        t.start();
        return t;
    }

    public void waitFor(Object future, long timeout) {
        Thread t = (Thread) future;
        try {
            t.join(timeout);
        } catch (InterruptedException ex) {
            throw new BitronixRuntimeException("job interrupted", ex);
        }
    }

    public boolean isDone(Object future) {
        Thread t = (Thread) future;
        return !t.isAlive();
    }

    public boolean isUsable() {
        return true;
    }

    public void shutdown() {
    }
}
