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
