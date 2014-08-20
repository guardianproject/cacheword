
package info.guardianproject.cacheword.test;

import android.test.AndroidTestCase;

import info.guardianproject.cacheword.Constants;
import info.guardianproject.cacheword.KDFIterationCalibrator;
import info.guardianproject.cacheword.PassphraseSecretsImpl;

import java.security.GeneralSecurityException;

/**
 * Test suite for the PBKDF iteration calibrator, adapted from Briar Project's
 * briar-core (relicensed with permission) Copyright (C) 2013 Sublime Software
 * Ltd
 */
public class KDFIterationCalibratorTest extends AndroidTestCase {

    private final static String TAG = "KDFIterationCalibratorTest";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCalibration() throws GeneralSecurityException {
        KDFIterationCalibrator calibrator = new KDFIterationCalibrator(
                Constants.PBKDF2_ITER_SAMPLES);

        // If the target time is unachievable, one iteration should be used
        int iterations = calibrator.chooseIterationCount(0);
        assertEquals(1, iterations);

        // If the target time is long, more than one iteration should be used
        iterations = calibrator.chooseIterationCount(10 * 1000);
        assertTrue(iterations > 1);

        // If the target time is very long, max iterations should be used
        iterations = calibrator.chooseIterationCount(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, iterations);
    }

    /**
     * JCE can be obtuse and obscure so lets make obviously sure we're ontop of
     * the algo we're actually using.
     * 
     * @throws GeneralSecurityException
     */
    public void testKDFMethodsSameness() throws GeneralSecurityException {
        PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();

        KDFIterationCalibrator calibrator = new KDFIterationCalibrator(
                Constants.PBKDF2_ITER_SAMPLES);
        char[] passphrase = "password".toCharArray();
        byte[] salt = crypto.generateSalt(Constants.PBKDF2_SALT_LEN_BYTES);
        int iterations = 1000;

        byte[] val1 = crypto.hashPassphrase(passphrase, salt, iterations).getEncoded();
        byte[] val2 = calibrator.pbkdf2_jce(passphrase, salt, iterations);
        byte[] val3 = calibrator.pbkdf2_bouncy(passphrase, salt, iterations);

        assertEquals(val1.length, val2.length);
        assertEquals(val1.length, val3.length);

        for (int i = 0; i < val1.length; i++) {
            assertEquals(val1[i], val2[i]);
            assertEquals(val1[i], val3[i]);
        }
    }
}
