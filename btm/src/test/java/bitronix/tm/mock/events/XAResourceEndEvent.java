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

import javax.transaction.xa.Xid;

/**
 *
 * @author lorban
 */
public class XAResourceEndEvent extends XAEvent {

    private int flag;

    public XAResourceEndEvent(Object source, Xid xid, int flag) {
        super(source, xid);
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }

    public String toString() {
        return "XAResourceEndEvent at " + getTimestamp() + " with flag=" + Decoder.decodeXAResourceFlag(flag) + " on " + getXid();
    }

}
