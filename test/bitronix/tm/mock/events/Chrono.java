package bitronix.tm.mock.events;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class Chrono {

    private static long lastTime = 0;
    private static long counter = 0;

    public synchronized static long getTime() {
        long time = System.currentTimeMillis();
        if (time <= lastTime) {
            counter++;
            time += counter;
            lastTime += counter;
        }
        else {
            counter = 0;
            lastTime = time;
        }
        return time;
    }

}
