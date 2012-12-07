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

import bitronix.tm.resource.jdbc.JdbcPooledConnection;

/**
 *
 * @author lorban
 */
public class ConnectionQueuedEvent extends Event {

    private JdbcPooledConnection jdbcPooledConnection;

    public ConnectionQueuedEvent(Object source, JdbcPooledConnection jdbcPooledConnection) {
        super(source, null);
        this.jdbcPooledConnection = jdbcPooledConnection;
    }

    public ConnectionQueuedEvent(Object source, Exception ex) {
        super(source, ex);
    }

    public JdbcPooledConnection getPooledConnectionImpl() {
        return jdbcPooledConnection;
    }

    public String toString() {
        return "ConnectionQueuedEvent at " + getTimestamp() + " on " + jdbcPooledConnection + (getException()!=null ? " and " + getException().toString() : "");
    }
}
