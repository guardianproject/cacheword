
package info.guardianproject.cacheword;

import android.content.Context;
import android.util.Log;

import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Represents a single 256 AES secret encrypted with a key derived from the user's passphrase.
 * This class handles the PBE key derivation, secret key generation, encryption, and persistence.
 * It also provides a means for fetching (decrypting) the secrets and changing the passphrase.
 *
 * This is the simplest of cases where the application's only secret is the user's
 * passphrase. We do not want to store the passphrase, nor a hash of the
 * passphrase on disk.
 *
 * Initialization process consists of:
 * <ol>
 * <li>1. Run the password through PBKDF2 with a random salt
 * <li>2. Generate a random 256 bit AES key with a random IV
 * <li>3. Use the derived key to encrypt the AES key in GCM mode
 * <li>4. Write the ciphertext, iv, and salt to disk
 * </ol>
 *
 * The exact data written to disk is represented by the SerializedSecretsV1 class.
 *
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
     * Generates a random AES key and encrypts it with a PBKDF2 key derived from x_passphrase. The resulting
     * ciphertext is saved to disk. All sensitive variables are wiped.
     * @param ctx
     * @param x_passphrase
     * @return
     */
    public static PassphraseSecrets initializeSecrets(Context ctx, char[] x_passphrase) {
        PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();
        try {
            SecretKeySpec secretKey = (SecretKeySpec) crypto.generateSecretKey();
            boolean saved           = encryptAndSave(ctx, x_passphrase, secretKey.getEncoded());
            SecretsManager.setInitialized(ctx, saved);

            if( saved ) return new PassphraseSecrets(secretKey);
            else        return null;
        } catch (GeneralSecurityException e ) {
            Log.e(TAG, "initializeSecrets failed: " +e.getClass().getName() + " : " + e.getMessage());
            return null;
        } finally {
            Wiper.wipe(x_passphrase);
        }
    }

    /**
     * Attempts to decrypt the stored secrets with x_passphrase. If successful, returns a PassphraseSecrets
     * initialized with the secret key.
     * @param ctx
     * @param x_passphrase WIPED
     * @return
     * @throws GeneralSecurityException
     */
    public static PassphraseSecrets fetchSecrets(Context ctx, char[] x_passphrase)
            throws GeneralSecurityException {
        PassphraseSecretsImpl crypto  = new PassphraseSecretsImpl();
        byte[] preparedSecret         = SecretsManager.getBytes(ctx, Constants.SHARED_PREFS_SECRETS);
        byte[] x_rawSecretKey         = null;

        try {
            x_rawSecretKey = crypto.decryptWithPassphrase(x_passphrase, preparedSecret);

            return new PassphraseSecrets(x_rawSecretKey);
        } finally {
            Wiper.wipe(x_passphrase);
            Wiper.wipe(x_rawSecretKey);
        }
    }

    /**
     * Re-encrypts the secret key in current_secrets with a new derived key from x_new_passphrase. The resulting
     * ciphertext is saved to disk.
     * @param ctx
     * @param current_secrets NOT WIPED
     * @param x_new_passphrase WIPED
     * @return
     */
    public static PassphraseSecrets changePassphrase(Context ctx, PassphraseSecrets current_secrets, char[] x_new_passphrase ) {
        byte[] x_rawSecretKey = null;
        try {
            x_rawSecretKey      = current_secrets.getSecretKey().getEncoded();
            boolean saved       = encryptAndSave(ctx, x_new_passphrase, x_rawSecretKey);

            if (saved) return current_secrets;
            else       return null;
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "changePassphrase failed: " +e.getClass().getName() + " : " + e.getMessage());
            return null;
        } finally {
            Wiper.wipe(x_new_passphrase);
            Wiper.wipe(x_rawSecretKey);
        }
    }
    /**
     * Encrypts the plaintext with the passphrase and saves the ciphertext bundle to disk.
     * @param ctx
     * @param x_passphrase the passphrase used to PBE on plaintext to NOT WIPED
     * @param x_plaintext the plaintext to encrypt NOT WIPED
     * @return
     * @throws GeneralSecurityException
     */
    private static boolean encryptAndSave(Context ctx, char[] x_passphrase, byte[] x_plaintext) throws GeneralSecurityException {
        PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();
        byte[] preparedSecret        = crypto.encryptWithPassphrase(ctx, x_passphrase, x_plaintext);
        boolean saved                = SecretsManager.saveBytes(ctx, Constants.SHARED_PREFS_SECRETS, preparedSecret);

        return saved;
    }



    @Override
    public void destroy() {
        Log.d(TAG, "destroy()");
        Wiper.wipe((SecretKeySpec)mSecretKey);
    }

}
