package bitronix.tm.mock.events;

import java.util.*;

/**
 * (c) Bitronix, 19-déc.-2005
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
