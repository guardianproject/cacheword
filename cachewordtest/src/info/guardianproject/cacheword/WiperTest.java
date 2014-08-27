
package info.guardianproject.cacheword;

import info.guardianproject.cacheword.Constants;
import info.guardianproject.cacheword.Wiper;

import junit.framework.TestCase;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class WiperTest extends TestCase {

    Random random;

    private final static byte BYTE_ZERO = (byte) 0;
    private final static byte CHAR_ZERO = '\0';

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        random = new Random();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testWipeBytes() {

        byte[] buf = new byte[50];
        random.nextBytes(buf);
        Wiper.wipe(buf);
        for (byte b : buf) {
            assertEquals(b, BYTE_ZERO);
        }
    }

    public void testWipeChars() {
        char[] buf = new BigInteger(50, random).toString(32).toCharArray();
        Wiper.wipe(buf);
        for (char b : buf) {
            assertEquals(b, CHAR_ZERO);
        }
    }

    public void testWipeByteBuffer() {

        byte[] buf = new byte[50];
        random.nextBytes(buf);
        ByteBuffer bb = ByteBuffer.wrap(buf);
        Wiper.wipe(bb);
        for (byte b : buf) {
            assertEquals(b, BYTE_ZERO);
        }
        while (bb.hasRemaining()) {
            assertEquals(bb.get(), BYTE_ZERO);
        }
    }

    public void testWipeSecretKeySpec() {
        byte[] buf = new byte[256];
        random.nextBytes(buf);
        SecretKeySpec spec = new SecretKeySpec(buf, "AES");

        byte[] raw = spec.getEncoded();
        assertTrue(Arrays.equals(buf, raw));

        Wiper.wipe(spec);
        byte[] wiped_raw = spec.getEncoded();
        assertFalse(Arrays.equals(buf, wiped_raw));

        assertEquals(raw.length, wiped_raw.length);
        for (int i = 0; i < raw.length; ++i) {
            assertEquals(BYTE_ZERO, wiped_raw[i]);
        }
    }

    public void testWipePBEKeySpec() throws NoSuchAlgorithmException {
        char[] password = "thisisapassword".toCharArray();
        byte[] salt = new byte[Constants.PBKDF2_SALT_LEN_BYTES];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(salt);
        PBEKeySpec x_spec = new PBEKeySpec(password, salt, 1000,
                Constants.PBKDF2_KEY_LEN_BITS);
        assertTrue(Arrays.equals(password, x_spec.getPassword()));

        Wiper.wipe(x_spec);

        try {
            assertFalse(Arrays.equals(password, x_spec.getPassword()));
            fail("PBEKeySpec.getPassword should throw an exception after being wiped");
        } catch (IllegalStateException e) {
            // pass
        }
    }

}
