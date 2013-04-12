package info.guardianproject.cacheword.test;

import info.guardianproject.cacheword.Constants;
import info.guardianproject.cacheword.Wiper;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Tests our memory wiping framework
 * @author abel
 */
public class WiperTest extends TestCase {

    public void testWipeByteArray() {
        try {
            byte[] wipe_me = new byte[100];
            byte[] zeros   = new byte[100];
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.nextBytes(wipe_me);
            Arrays.fill( zeros, (byte) 0 );
            assertFalse( Arrays.equals(wipe_me, zeros) );
            Wiper.wipe(wipe_me);
            assertTrue( Arrays.equals(wipe_me, zeros) );
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void testWipeCharArray() {
        char[] wipe_me = new String("falling squares fade seriously fast").toCharArray();
        char[] zeros   = new char[wipe_me.length];
        Arrays.fill(zeros, '\0');
        assertFalse( Arrays.equals(wipe_me, zeros) );
        System.out.println(new String(wipe_me));
        System.out.println(new String(zeros));
        Wiper.wipe(wipe_me);
        System.out.println(new String(wipe_me));
        System.out.println(new String(zeros));
        assertTrue( Arrays.equals(wipe_me, zeros) );
    }

    public void testWipeByteBuffer() {
        try {
            byte[] wipe_me = new byte[100];
            byte[] zeros   = new byte[100];
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.nextBytes(wipe_me);
            Arrays.fill( zeros, (byte) 0 );

            ByteBuffer wipe_me_too = ByteBuffer.wrap(wipe_me);
            assertFalse( Arrays.equals(wipe_me_too.array(), zeros) );
            assertFalse( Arrays.equals(wipe_me, zeros) );
            Wiper.wipe(wipe_me_too);
            assertTrue( Arrays.equals(wipe_me_too.array(), zeros) );
            assertTrue( Arrays.equals(wipe_me, zeros) );
        } catch (NoSuchAlgorithmException e) {
            fail("NoSuchAlgorithmException");
            e.printStackTrace();
        }
    }

    public void testWipeCharBuffer() {
        char[] wipe_me = "godly dangers grapple gorgeous daggers".toCharArray();
        char[] zeros   = new char[wipe_me.length];
        Arrays.fill(zeros, '\0');
        CharBuffer wipe_me_too = CharBuffer.wrap(wipe_me);
        assertFalse( Arrays.equals(wipe_me_too.array(), zeros) );
        assertFalse( Arrays.equals(wipe_me, zeros) );
        Wiper.wipe(wipe_me_too);
        assertTrue( Arrays.equals(wipe_me_too.array(), zeros) );
        assertTrue( Arrays.equals(wipe_me, zeros) );
    }

    public void testWipeSecretKeySpec() {
        try {

            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(Constants.AES_KEY_LENGTH);

            SecretKeySpec wipe_me = (SecretKeySpec) generator.generateKey();

            byte[] secret_data = wipe_me.getEncoded();
            byte[] zeros   = new byte[secret_data.length];
            Arrays.fill(zeros, (byte) 0);
            assertFalse( Arrays.equals(secret_data, zeros) );
            assertTrue( Arrays.equals(secret_data, wipe_me.getEncoded()) );
            Wiper.wipe(wipe_me);
            assertTrue( Arrays.equals(wipe_me.getEncoded(), zeros) );
            assertFalse( Arrays.equals(secret_data, wipe_me.getEncoded()) );

        } catch (NoSuchAlgorithmException e) {
            fail("NoSuchAlgorithmException");
            e.printStackTrace();
        }
    }

    public void testWipePBEKeySpec() {
        char[] password = "hairy funnels hump floss hungrily".toCharArray();
        PBEKeySpec wipe_me = new PBEKeySpec(password, new byte[] {1,2,3}, Constants.PBKDF2_ITER_COUNT, Constants.PBKDF2_KEY_LEN);

        assertTrue( Arrays.equals(password, wipe_me.getPassword()) );
        Wiper.wipe(wipe_me);
        try {
            char[] recovered_pass = wipe_me.getPassword();
            fail("recovered_pass password :" + new String(recovered_pass));
        } catch ( IllegalStateException e ){
            /// Yay!
        }
    }

}
