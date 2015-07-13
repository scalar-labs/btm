package bitronix.tm.journal;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public final class ByteBufferUtil {

    private ByteBufferUtil() {
    }

    public static ByteBuffer createByteBuffer(final String input)
            throws UnsupportedEncodingException {
        final byte[] inputArray = input.getBytes("UTF-8");
        final ByteBuffer byteBuffer = ByteBuffer.wrap(inputArray);
        return byteBuffer;
    }

}
