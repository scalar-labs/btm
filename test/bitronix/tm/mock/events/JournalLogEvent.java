package bitronix.tm.mock.events;

import bitronix.tm.internal.Decoder;
import bitronix.tm.internal.Uid;

import java.util.Set;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class JournalLogEvent extends Event {

    private int status;
    private Uid gtrid;
    private Set jndiNames;


    public JournalLogEvent(Object source, int status, Uid gtrid, Set jndiNames) {
        super(source, null);
        this.status = status;
        this.gtrid = gtrid;
        this.jndiNames = jndiNames;
    }


    public int getStatus() {
        return status;
    }

    public Uid getGtrid() {
        return gtrid;
    }

    public Set getJndiNames() {
        return jndiNames;
    }

    public String toString() {
        return "JournalLogEvent at " + getTimestamp() + " with status=" + Decoder.decodeStatus(status);
    }
}
