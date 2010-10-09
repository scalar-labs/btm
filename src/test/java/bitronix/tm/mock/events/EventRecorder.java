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
package bitronix.tm.mock.events;

import java.util.*;

/**
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
