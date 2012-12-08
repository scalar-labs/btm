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

/**
 *
 * @author Ludovic Orban
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
