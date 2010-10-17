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
package bitronix.tm.mock.resource;

import javax.transaction.xa.Xid;

/**
 *
 * @author lorban
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
