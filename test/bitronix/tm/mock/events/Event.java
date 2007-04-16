package bitronix.tm.mock.events;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public abstract class Event {

    private Exception callStack;
    private Object source;
    private Exception exception;
    private long timestamp;

    protected Event(Object source, Exception ex) {
        this.callStack = new Exception();
        this.source = source;
        this.exception = ex;
        this.timestamp = Chrono.getTime();
    }

    public Exception getCallStack() {
        return callStack;
    }

    public Object getSource() {
        return source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Exception getException() {
        return exception;
    }

}
