package bitronix.tm.journal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class InterruptService {

    private final InterruptableThreadList interruptableThreadList;
    private final AtomicLong successfulInterrupts = new AtomicLong();
    private ExecutorService executorService;
    private final AtomicBoolean run = new AtomicBoolean(true);

    public InterruptService(final InterruptableThreadList interruptableThreadList) {
        if (interruptableThreadList == null) {
            throw new NullPointerException("threadList cannot be null");
        }
        this.interruptableThreadList = interruptableThreadList;
    }

    public void start() {
        executorService = Executors.newSingleThreadExecutor();
        final Runnable interrupter = new Runnable() {
            @Override
            public void run() {
                while (run.get()) {
                    final boolean successfulInterrupt = interruptableThreadList
                            .interruptRandomThread();
                    if (successfulInterrupt) {
                        successfulInterrupts.incrementAndGet();
                    }
                }
            }
        };
        executorService.submit(interrupter);
    }

    public void stop() throws InterruptedException {
        run.set(false);
        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(2,
                TimeUnit.SECONDS);
        if (!terminated) {
            throw new IllegalStateException("termination");
        }
    }

    public long getSuccessfulInterrupts() {
        return successfulInterrupts.get();
    }
}
