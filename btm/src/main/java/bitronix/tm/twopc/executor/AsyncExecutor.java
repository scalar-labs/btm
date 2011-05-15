/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Bitronix Software.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This implementation executes submitted jobs using a <code>java.util.concurrent</code> cached thread pool.
 *
 * @author lorban
 */
public class AsyncExecutor implements Executor {

    private final ExecutorService executorService;


    public AsyncExecutor() {
        executorService = Executors.newCachedThreadPool();
    }

    public Object submit(Job job) {
        return executorService.submit(job);
    }

    public void waitFor(Object future, long timeout) {
        Future<?> f = (Future<?>) future;

        try {
            f.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new BitronixRuntimeException("job interrupted", ex);
        } catch (ExecutionException ex) {
            throw new BitronixRuntimeException("job execution exception", ex);
        } catch (TimeoutException ex) {
            // ok, just return
        }
    }

    public boolean isDone(Object future) {
        Future<?> f = (Future<?>) future;

        return f.isDone();
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
