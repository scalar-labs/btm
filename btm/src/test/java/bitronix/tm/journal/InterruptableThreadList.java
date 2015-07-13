package bitronix.tm.journal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class InterruptableThreadList {

    private final List<Thread> threads = new ArrayList<Thread>();

    public InterruptableThreadList() {
    }

    public synchronized void addCurrentThread() {
        final Thread currentThread = Thread.currentThread();
        threads.add(currentThread);
        Thread.yield();
    }

    public synchronized void removeCurrentThread() {
        final Thread currentThread = Thread.currentThread();
        threads.remove(currentThread);
    }

    /**
     *
     * @return true on successful interruption
     */
    public synchronized boolean interruptRandomThread() {
        if (threads.isEmpty()) {
            return false;
        }
        final Random random = new Random();
        final int threadIndex = random.nextInt(threads.size());

        final Thread thread = threads.get(threadIndex);
        thread.interrupt();
        return true;
    }
}
