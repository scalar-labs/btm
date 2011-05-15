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
package bitronix.tm.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A System.currentTimeMillis() replacement which guarantees monotonic time increment.
 *
 * @author lorban
 */
public class MonotonicClock {
    private static final AtomicLong lastTime = new AtomicLong();

    /**
     * Return the current time in milliseconds, guaranteeing monotonic time increment.
     * @return the current time in milliseconds.
     */
    public static long currentTimeMillis() {
        long now = System.nanoTime() / 1000000;
        long time = lastTime.get();
        if (now > time) {
            lastTime.compareAndSet(time, now);
            return lastTime.get();
        }
        return time;
    }
}
