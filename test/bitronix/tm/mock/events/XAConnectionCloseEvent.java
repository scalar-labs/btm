package bitronix.tm.mock.events;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class XAConnectionCloseEvent extends Event {

    public XAConnectionCloseEvent(Object source) {
        super(source, null);
    }

    public XAConnectionCloseEvent(Object source, Exception ex) {
        super(source, ex);
    }

    public String toString() {
        return "XAConnectionCloseEvent at " + getTimestamp() + (getException()!=null ? " and " + getException().toString() : "");
    }
}
