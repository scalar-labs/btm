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
package bitronix.tm.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A System.currentTimeMillis() replacement which guarantees monotonic time increment.
 *
 * @author Ludovic Orban
 */
public final class MonotonicClock {

    private static final AtomicLong lastTime = new AtomicLong(Long.MIN_VALUE);

    private MonotonicClock() {
    }

    /**
     * Return the current time in milliseconds, guaranteeing monotonic time increment.
     *
     * @return the current time in milliseconds.
     */
    public static long currentTimeMillis() {
        long nanoTime = System.nanoTime();
        System.out.println("nano time: " + nanoTime);
        long now = TimeUnit.NANOSECONDS.toMillis(nanoTime);
        System.out.println("now: " + now);
        long time = lastTime.get();
        System.out.println("time: " + time);
        if (now > time) {
            lastTime.compareAndSet(time, now);
            long lastTime = MonotonicClock.lastTime.get();
            System.out.println("lastTime: " + lastTime);
            return lastTime;
        }
        System.out.println("time: " + time);
        return time;
    }
}
