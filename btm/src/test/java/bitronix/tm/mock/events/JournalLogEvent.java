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
