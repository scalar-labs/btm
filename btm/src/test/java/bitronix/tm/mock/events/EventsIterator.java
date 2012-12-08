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

import java.util.Iterator;
import java.util.Map;

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
