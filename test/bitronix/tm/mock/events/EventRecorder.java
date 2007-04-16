package bitronix.tm.mock.events;

import java.util.*;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class EventRecorder {

    private static Map eventRecorders = new HashMap();

    public synchronized static EventRecorder getEventRecorder(Object key) {
        EventRecorder er = (EventRecorder) eventRecorders.get(key);
        if (er == null) {
            er = new EventRecorder();
            eventRecorders.put(key, er);
        }
        return er;
    }

    public static Map getEventRecorders() {
        return eventRecorders;
    }

    public static Iterator iterateEvents() {
        return new EventsIterator(eventRecorders);
    }

    public static List getOrderedEvents() {
        Iterator iterator = iterateEvents();
        List orderedEvents = new ArrayList();
        while (iterator.hasNext()) {
            Object o = iterator.next();
            orderedEvents.add(o);
        }
        return orderedEvents;
    }

    public static String dumpToString() {
        StringBuffer sb = new StringBuffer();

        int i = 0;
        Iterator it = iterateEvents();
        while (it.hasNext()) {
            Event event = (Event) it.next();
            sb.append(i++);
            sb.append(" - ");
            sb.append(event.toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    public static void clear() {
        eventRecorders.clear();
    }

    private List events = new ArrayList();

    private EventRecorder() {
    }

    public void addEvent(Event evt) {
        events.add(evt);
    }

    public List getEvents() {
        return events;
    }

}
