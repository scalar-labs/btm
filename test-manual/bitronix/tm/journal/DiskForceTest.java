package bitronix.tm.journal;

import junit.framework.TestCase;

import java.io.*;
import java.util.ArrayList;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class DiskForceTest extends TestCase {

    private int count = 100;

    public void testRandomAccessFile() throws Exception {
        RandomAccessFile raf = new RandomAccessFile("test.dat", "rw");

        ArrayList results = new ArrayList();

        byte[] buffer = new byte[512];

        for (int i=0; i<count ;i++) {
            long before = System.currentTimeMillis();
            raf.write(buffer);
            raf.seek(0L);
            raf.getFD().sync();
            long after = System.currentTimeMillis();
            long duration = after - before;
            results.add(new Long(duration));
        }

        long zeros = 0;
        long lower = Long.MAX_VALUE;
        long higher = 0;
        long total = 0;
        for (int i = 0; i < results.size(); i++) {
            Long aLong = (Long) results.get(i);
            long val = aLong.longValue();

            total += val;
            if (val == 0) {
                zeros++;
                continue;
            }

            lower = Math.min(lower, val);
            higher = Math.max(higher, val);
        }

        System.out.println("zeros: " + zeros);
        System.out.println("lower: " + lower);
        System.out.println("higher: " + higher);
        System.out.println("total: " + total);
        System.out.println("avg: " + ((double)total/count));

        raf.close();
    }

    public void testBufferedStreams() throws Exception {
        File file = new File("test.dat");
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        DataOutputStream dos = new DataOutputStream(bos);

        ArrayList results = new ArrayList();

        byte[] buffer = new byte[512];

        for (int i=0; i<count ;i++) {
            long before = System.currentTimeMillis();
            dos.write(buffer);
            fos.getChannel().position(0L);
            dos.flush();
            fos.getFD().sync();
            long after = System.currentTimeMillis();
            long duration = after - before;
            results.add(new Long(duration));
        }

        long zeros = 0;
        long lower = Long.MAX_VALUE;
        long higher = 0;
        long total = 0;
        for (int i = 0; i < results.size(); i++) {
            Long aLong = (Long) results.get(i);
            long val = aLong.longValue();

            total += val;
            if (val == 0) {
                zeros++;
                continue;
            }

            lower = Math.min(lower, val);
            higher = Math.max(higher, val);
        }

        System.out.println("zeros: " + zeros);
        System.out.println("lower: " + lower);
        System.out.println("higher: " + higher);
        System.out.println("total: " + total);
        System.out.println("avg: " + ((double)total/count));

        dos.close();
    }

    public void testNioBuffers() throws Exception {
        File file = new File("test.dat");
        FileOutputStream fos = new FileOutputStream(file);
        FileChannel channel = fos.getChannel();




        ArrayList results = new ArrayList();

        ByteBuffer buffer = ByteBuffer.allocate(512);

        for (int i=0; i<count ;i++) {
            long before = System.currentTimeMillis();
            channel.write(buffer, 0L);
            channel.force(true);
            long after = System.currentTimeMillis();
            long duration = after - before;
            results.add(new Long(duration));
        }

        long zeros = 0;
        long lower = Long.MAX_VALUE;
        long higher = 0;
        long total = 0;
        for (int i = 0; i < results.size(); i++) {
            Long aLong = (Long) results.get(i);
            long val = aLong.longValue();

            total += val;
            if (val == 0) {
                zeros++;
                continue;
            }

            lower = Math.min(lower, val);
            higher = Math.max(higher, val);
        }

        System.out.println("zeros: " + zeros);
        System.out.println("lower: " + lower);
        System.out.println("higher: " + higher);
        System.out.println("total: " + total);
        System.out.println("avg: " + ((double)total/count));

        fos.close();
    }

}
