
package info.guardianproject.cacheword;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * A utility class for securely wiping memory locations and handling sensitive
 * strings as character arrays.
 */
public class Wiper {

    // On android the default is UTF-8
    public final static Charset Utf8CharSet = Charset.forName("UTF-8");

    /**
     * Fills the parameter with 0s
     */
    public static void wipe(byte[] bytes) {
        Arrays.fill(bytes, (byte) 0);
    }

    /**
     * Fills the parameter with 0s
     */
    public static void wipe(char[] chars) {
        Arrays.fill(chars, '\0');
    }

    /**
     * Fills the underlying array with 0s
     */
    public static void wipe(ByteBuffer bb) {
        wipe(bb.array());
    }

    /**
     * Fills the underlying array with 0s
     */
    public static void wipe(CharBuffer cb) {
        wipe(cb.array());
    }

    /**
     * Convert a CharBuffer into a UTF-8 encoded ByteBuffer
     */
    public static ByteBuffer utf8ToBytes(CharBuffer cb) {
        return Utf8CharSet.encode(cb);
    }

    /**
     * Securely convert a char[] to a UTF-8 encoded byte[]. All intermediate
     * memory is wiped.
     *
     * @return a new byte array containing the encoded characters
     */
    public static byte[] utf8charsToBytes(char[] chars) {
        ByteBuffer bb = utf8ToBytes(CharBuffer.wrap(chars));
        byte[] result = new byte[bb.limit()];
        System.arraycopy(bb.array(), 0, result, 0, bb.limit());
        wipe(bb.array());
        return result;
    }

    /**
     * Securely convert a UTF-8 encoded byte[] to a char[] All intermediate
     * memory is wiped.
     *
     * @return a new char array containing the decoded characters
     */
    public static char[] bytesToUtf8Chars(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        CharBuffer cb = Utf8CharSet.decode(bb);
        char[] result = new char[cb.limit()];
        System.arraycopy(cb.array(), 0, result, 0, cb.limit());
        wipe(cb.array());
        return result;
    }
}
