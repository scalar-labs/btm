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
public class EventsIterator implements Iterator {

    private Iterator[] eventRecorderIterators;
    private Event[] nextEvents;

    public EventsIterator(Map eventRecorders) {
        int size = eventRecorders.size();
        eventRecorderIterators = new Iterator[size];
        nextEvents = new Event[size];

        int i = 0;
        Iterator it = eventRecorders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            EventRecorder er = (EventRecorder) entry.getValue();
            eventRecorderIterators[i] = er.getEvents().iterator();
            if (eventRecorderIterators[i].hasNext())
                nextEvents[i] = (Event) eventRecorderIterators[i].next();
            i++;
        }
    }

    public void remove() {
    }

    public boolean hasNext() {
        for (int i = 0; i < nextEvents.length; i++) {
            Event nextEvent = nextEvents[i];
            if (nextEvent != null)
                return true;
        }
        return false;
    }

    public Object next() {
        Event current = null;
        int index = -1;
        for (int i = 0; i < nextEvents.length; i++) {
            Event nextEvent = nextEvents[i];
            if (nextEvent == null)
                continue;
            
            if (current == null) {
                current = nextEvent;
                index = i;
            }
            else if (nextEvent.getTimestamp() < current.getTimestamp()) {
                current = nextEvent;
                index = i;
            }
        }

        if (index != -1) {
            if (eventRecorderIterators[index].hasNext())
                nextEvents[index] = (Event) eventRecorderIterators[index].next();
            else
                nextEvents[index] = null;
        }

        return current;
    }

}
