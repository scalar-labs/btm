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
