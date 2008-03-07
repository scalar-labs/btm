package bitronix.tm.mock.events;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class LocalCommitEvent extends Event {

    public LocalCommitEvent(Object source, Exception ex) {
        super(source, ex);
    }

    public String toString() {
        return "LocalCommitEvent at " + getTimestamp();
    }

}
