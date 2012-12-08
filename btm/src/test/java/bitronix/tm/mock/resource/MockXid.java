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
package bitronix.tm.mock.resource;

import javax.transaction.xa.Xid;

/**
 *
 * @author Ludovic Orban
 */
public class MockXid implements Xid {

    private int formatId = 123456;
    private byte[] bqual;
    private byte[] gtrid;

    public MockXid(long bqual0, long gtrid0, int formatId) {
        this(bqual0, gtrid0);
        this.formatId = formatId;
    }

    public MockXid(long bqual0, byte[] gtrid0, int formatId) {
        this.bqual = new byte[8];
        System.arraycopy(longToBytes(bqual0), 0, bqual, 0, 8);
        this.gtrid = gtrid0;
        this.formatId = formatId;
    }

    public MockXid(long bqual0, long gtrid0) {
        this.bqual = new byte[8];
        this.gtrid = new byte[8];
        System.arraycopy(longToBytes(bqual0), 0, bqual, 0, 8);
        System.arraycopy(longToBytes(gtrid0), 0, gtrid, 0, 8);
    }

    public MockXid(byte[] bqual, byte[] gtrid) {
        this.bqual = bqual;
        this.gtrid = gtrid;
    }

    public MockXid(byte[] bqual, byte[] gtrid, int formatId) {
        this.bqual = bqual;
        this.gtrid = gtrid;
        this.formatId = formatId;
    }

    public int getFormatId() {
        return formatId;
    }

    public byte[] getBranchQualifier() {
        return bqual;
    }

    public byte[] getGlobalTransactionId() {
        return gtrid;
    }

    private static byte[] longToBytes(long aLong) {
        byte[] array = new byte[8];

        for (int i = 0; i < 8; i++) {
            array[i] = (byte) ((aLong >> (8 * i)) & 0xff);
        }

        return array;
    }
}
