
package info.guardianproject.cacheword;

import android.content.Context;
import android.util.Log;

import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Represents a single 256 AES secret encrypted with a key derived from the
 * user's passphrase. This class handles the PBE key derivation, secret key
 * generation, encryption, and persistence. It also provides a means for
 * fetching (decrypting) the secrets and changing the passphrase. This is the
 * simplest of cases where the application's only secret is the user's
 * passphrase. We do not want to store the passphrase, nor a hash of the
 * passphrase on disk. Initialization process consists of:
 * <ol>
 * <li>1. Run the password through PBKDF2 with a random salt
 * <li>2. Generate a random 256 bit AES key with a random IV
 * <li>3. Use the derived key to encrypt the AES key in GCM mode
 * <li>4. Write the ciphertext, iv, and salt to disk
 * </ol>
 * The exact data written to disk is represented by the SerializedSecretsV1
 * class.
 */
public class PassphraseSecrets implements ICachedSecrets {

    private static final String TAG = "PassphraseSecrets";
    private final SecretKey mSecretKey;

    private PassphraseSecrets(byte[] key) throws GeneralSecurityException {
        mSecretKey = new SecretKeySpec(key, "AES");
    }

    private PassphraseSecrets(SecretKey key) throws GeneralSecurityException {
        mSecretKey = key;
    }

    /**
     * Retrieve the AES secret key
     */
    public SecretKey getSecretKey() {
        return mSecretKey;
    }

    /**
     * Generates a random AES key and encrypts it with a PBKDF2 key derived from
     * x_passphrase. The resulting ciphertext is saved to disk. All sensitive
     * variables are wiped.
     * 
     * @param ctx
     * @param x_passphrase
     * @return
     */
    public static PassphraseSecrets initializeSecrets(Context ctx, char[] x_passphrase) {
        PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();
        try {
            SecretKeySpec secretKey = (SecretKeySpec) crypto.generateSecretKey();
            boolean saved = encryptAndSave(ctx, x_passphrase, secretKey.getEncoded());
            SecretsManager.setInitialized(ctx, saved);

            if (saved)
                return new PassphraseSecrets(secretKey);
            else
                return null;
        } catch (GeneralSecurityException e) {
            Log.e(TAG,
                    "initializeSecrets failed: " + e.getClass().getName() + " : " + e.getMessage());
            return null;
        } finally {
            Wiper.wipe(x_passphrase);
        }
    }

    /**
     * Attempts to decrypt the stored secrets with x_passphrase. If successful,
     * returns a PassphraseSecrets initialized with the secret key.
     * 
     * @param ctx
     * @param x_passphrase WIPED
     * @return
     * @throws GeneralSecurityException
     */
    public static PassphraseSecrets fetchSecrets(Context ctx, char[] x_passphrase)
            throws GeneralSecurityException {
        byte[] preparedSecret = SecretsManager.getBytes(ctx, Constants.SHARED_PREFS_SECRETS);
        SerializedSecretsV1 ss = new SerializedSecretsLoader().loadSecrets(preparedSecret);
        byte[] x_rawSecretKey = null;

        try {
            PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();
            x_rawSecretKey = crypto.decryptWithPassphrase(x_passphrase, ss);
            PassphraseSecrets ps = new PassphraseSecrets(x_rawSecretKey);

            // check for insecure iteration counts and upgrade if necessary
            // we do this by "changing" the passphrase to the same passphrase
            // since changePassphrase calls calibrateKDF()
            if (ss.pbkdf_iter_count < getPBKDF2MinimumIterationCount(ctx)) {
                ps = changePassphrase(ctx, ps, x_passphrase);
                if (ps == null)
                    throw new GeneralSecurityException(
                            "Upgrading iteration count failed during save");
            }
            return ps;
        } finally {
            Wiper.wipe(x_passphrase);
            Wiper.wipe(x_rawSecretKey);
        }
    }

    /**
     * Re-encrypts the secret key in current_secrets with a new derived key from
     * x_new_passphrase. The resulting ciphertext is saved to disk.
     * 
     * @param ctx
     * @param current_secrets NOT WIPED
     * @param x_new_passphrase WIPED
     * @return
     */
    public static PassphraseSecrets changePassphrase(Context ctx,
            PassphraseSecrets current_secrets, char[] x_new_passphrase) {
        byte[] x_rawSecretKey = null;
        try {
            x_rawSecretKey = current_secrets.getSecretKey().getEncoded();
            boolean saved = encryptAndSave(ctx, x_new_passphrase, x_rawSecretKey);

            if (saved)
                return current_secrets;
            else
                return null;
        } catch (GeneralSecurityException e) {
            Log.e(TAG,
                    "changePassphrase failed: " + e.getClass().getName() + " : " + e.getMessage());
            return null;
        } finally {
            Wiper.wipe(x_new_passphrase);
            Wiper.wipe(x_rawSecretKey);
        }
    }

    /**
     * Encrypts the plaintext with the passphrase and saves the ciphertext
     * bundle to disk.
     * 
     * @param ctx
     * @param x_passphrase the passphrase used to PBE on plaintext to NOT WIPED
     * @param x_plaintext the plaintext to encrypt NOT WIPED
     * @return
     * @throws GeneralSecurityException
     */
    private static boolean encryptAndSave(Context ctx, char[] x_passphrase, byte[] x_plaintext)
            throws GeneralSecurityException {
        PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();
        int pbkdf2_iter_count = calibrateKDF(ctx);
        SerializedSecretsV1 ss = crypto.encryptWithPassphrase(ctx, x_passphrase, x_plaintext,
                pbkdf2_iter_count);
        byte[] preparedSecret = ss.concatenate();
        boolean saved = SecretsManager.saveBytes(ctx, Constants.SHARED_PREFS_SECRETS,
                preparedSecret);

        return saved;
    }

    private static int getPBKDF2MinimumIterationCount(Context ctx) {
        int iter_count = ctx.getResources().getInteger(
                R.integer.cacheword_pbkdf2_minimum_iteration_count);
        return iter_count;
    }

    private static boolean getPBKDF2AutoCalibrationEnabled(Context ctx) {
        boolean calibrate = ctx.getResources().getBoolean(R.bool.cacheword_pbkdf2_auto_calibrate);
        return calibrate;
    }

    /**
     * returns the number of iterations to use
     * 
     * @throws GeneralSecurityException
     */
    private static int calibrateKDF(Context ctx) throws GeneralSecurityException {
        int minimum = getPBKDF2MinimumIterationCount(ctx);
        if (getPBKDF2AutoCalibrationEnabled(ctx)) {
            KDFIterationCalibrator calibrator = new KDFIterationCalibrator(
                    Constants.PBKDF2_ITER_SAMPLES);
            int calculated = calibrator.chooseIterationCount(1000);
            int iterations = Math.max(minimum, calculated);
            Log.d(TAG, "calibrateKDF() selected: " + iterations);
            return iterations;
        } else {
            // if auto calibration isn't enabled we use the minimum
            return minimum;
        }
    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroy()");
        Wiper.wipe((SecretKeySpec) mSecretKey);
    }

}
