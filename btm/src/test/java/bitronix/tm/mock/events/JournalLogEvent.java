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

import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.Uid;

import java.util.Set;

/**
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
