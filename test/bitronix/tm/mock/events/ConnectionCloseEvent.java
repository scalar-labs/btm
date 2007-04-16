package bitronix.tm.mock.events;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class ConnectionCloseEvent extends Event {

    public ConnectionCloseEvent(Object source) {
        super(source, null);
    }

    public ConnectionCloseEvent(Object source, Exception ex) {
        super(source, ex);
    }

    public String toString() {
        return "ConnectionCloseEvent at " + getTimestamp() + (getException()!=null ? " and " + getException().toString() : "");
    }
}
