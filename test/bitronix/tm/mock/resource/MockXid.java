package bitronix.tm.mock.resource;

import javax.transaction.xa.Xid;

/**
 * (c) Bitronix, 26-déc.-2005
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

    public int getFormatId() {
        return formatId;
    }

    public byte[] getBranchQualifier() {
        return bqual;
    }

    public byte[] getGlobalTransactionId() {
        return gtrid;
    }

    public String toString() {
        return gtrid + " - " + bqual;
    }

    private static byte[] longToBytes(long aLong) {
        byte[] array = new byte[8];

        for (int i = 0; i < 8; i++) {
            array[i] = (byte) ((aLong >> (8 * i)) & 0xff);
        }

        return array;
    }
}
