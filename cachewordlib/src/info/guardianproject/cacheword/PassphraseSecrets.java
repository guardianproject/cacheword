
package info.guardianproject.cacheword;

import android.content.Context;
import android.util.Log;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The simplest of cases where the application's only secret is the user's
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
     * Generates a ranndom AES key, and encrypts it with a PBE key derived from x_passphrase. The resulting
     * ciphertext is saved to disk with PassphraseSecrets.encryptAndSave
     * @param ctx
     * @param x_passphrase
     * @return
     */
    public static PassphraseSecrets initializeSecrets(Context ctx, char[] x_passphrase) {
        try {
            SecretKey secretKey = generateSecretKey();
            boolean saved       = encryptAndSave(ctx, x_passphrase, secretKey.getEncoded());
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
     * @param x_passphrase
     * @return
     * @throws GeneralSecurityException
     */
    public static PassphraseSecrets fetchSecrets(Context ctx, char[] x_passphrase)
            throws GeneralSecurityException {
        byte[] x_rawSecretKey = null;
        try {
            byte[] preparedSecret  = SecretsManager.getBytes(ctx, Constants.SHARED_PREFS_SECRETS);
            SerializedSecretsV1 ss = new SerializedSecretsV1(preparedSecret);
            ss.parse();

            byte[] salt                   = ss.salt;
            byte[] iv                     = ss.iv;
            byte[] ciphertext             = ss.ciphertext;
//            int    version                = ss.version; // TODO unused for now
            SecretKeySpec x_passphraseKey = hashPassphrase(x_passphrase, salt);
            x_rawSecretKey                = decryptSecretKey(x_passphraseKey, iv, ciphertext);

            return new PassphraseSecrets(x_rawSecretKey);
        } finally {
            Wiper.wipe(x_passphrase);
            Wiper.wipe(x_rawSecretKey);
        }
    }

    /**
     * Re-encrypts the secret key in current_secrets with a new derived key from x_new_passphrase. The resulting
     * ciphertext is saved to disk with PassphraseSecrets.encryptAndSave
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

    //used by initialization and change password routines

    /**
     * Derives an encryption key from x_passphrase, then uses this derived key to encrypt x_plaintext.
     * The resulting cipher text, plus meta data (version, salt, iv, @see SerializedSecretsV1) is saved to
     * disk in SharedPreferences.
     * @param ctx
     * @param x_passphrase the passphrase used to PBE on plaintext to NOT WIPED
     * @param x_plaintext the plaintext to encrypt NOT WIPED
     * @return
     * @throws GeneralSecurityException
     */
    private static boolean encryptAndSave(Context ctx, char[] x_passphrase, byte[] x_plaintext) throws GeneralSecurityException {
        SecretKeySpec x_passphraseKey = null;
        try {
            byte[] salt               = generateSalt(Constants.SALT_LENGTH);
            byte[] iv                 = generateIv(Constants.GCM_IV_LENGTH);
            x_passphraseKey           = hashPassphrase(x_passphrase, salt);
            byte[] encryptedSecretKey = encryptSecretKey(x_passphraseKey, iv, x_plaintext);
            SerializedSecretsV1 ss    = new SerializedSecretsV1(Constants.VERSION_ZERO, salt, iv, encryptedSecretKey);
            byte[] preparedSecret     = ss.concatenate();
            boolean saved             = SecretsManager.saveBytes(ctx, Constants.SHARED_PREFS_SECRETS, preparedSecret);

            return saved;
        } finally {
            Wiper.wipe(x_passphraseKey);
        }
    }

    // used by initialization and verification routines

    /**
     * Hash the password with PBKDF2 at Constants.PBKDF2_ITER_COUNT iterations
     * Does not wipe the password.
     * @param x_password
     * @param salt
     * @return the AES SecretKeySpec containing the hashed password
     * @throws GeneralSecurityException
     */
    private static SecretKeySpec hashPassphrase(char[] x_password, byte[] salt)
            throws GeneralSecurityException {
        PBEKeySpec x_spec = null;
        try {
            x_spec                   = new PBEKeySpec(x_password, salt, Constants.PBKDF2_ITER_COUNT, Constants.PBKDF2_KEY_LEN);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            return new SecretKeySpec(factory.generateSecret(x_spec).getEncoded(), "AES");
        } finally {
            Wiper.wipe(x_spec);
        }
    }

    // verification routines: used to unlock secrets

    /**
     * Decrypt the supplied cipher text with AES GCM
     * Does not wipe the key nor the plaintext
     * @param x_passphraseKey
     * @param iv
     * @param ciphertext
     * @return the plaintext
     * @throws GeneralSecurityException on MAC failure or wrong key
     */
    private static byte[] decryptSecretKey(SecretKey x_passphraseKey, byte[] iv, byte[] ciphertext)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, x_passphraseKey, new IvParameterSpec(iv));

        return cipher.doFinal(ciphertext);
    }

    // initialization routines: creates secrets

    /**
     * Encrypts the data with AES GSM
     * Does not wipe the data nor the key
     * @param x_passphraseKey
     * @param iv
     * @param data
     * @return the encrypted key ciphertext
     * @throws GeneralSecurityException
     */
    private static byte[] encryptSecretKey(SecretKey x_passphraseKey, byte[] iv, byte[] data)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // TODO(abel) follow this rabbit hole down and wipe it!
        cipher.init(Cipher.ENCRYPT_MODE, x_passphraseKey, new IvParameterSpec(iv));

        return cipher.doFinal(data);
    }

    private static byte[] generateIv(int length) throws NoSuchAlgorithmException {
        byte[] iv = new byte[length];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(iv);
        return iv;
    }

    private static byte[] generateSalt(int length) throws NoSuchAlgorithmException {
        byte[] salt = new byte[length];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(salt);
        return salt;
    }

    /**
     * Generate a random AES_KEY_LENGTH bit AES key
     */
    private static SecretKey generateSecretKey() {
        try {

            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(Constants.AES_KEY_LENGTH);

            return generator.generateKey();

        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Simple wrapper class to encapsulate array manipulation
     *
     * This class does not handle sensitive data.
     */
    public static class SerializedSecretsV1 {
        public int version;
        public byte[] salt;
        public byte[] iv;
        public byte[] ciphertext;
        public byte[] serialized;

        public SerializedSecretsV1(int version, byte[] salt, byte[] iv, byte[] ciphertext) {
            this.version = version;
            this.salt = salt;
            this.iv = iv;
            this.ciphertext = ciphertext;
        }

        public SerializedSecretsV1(byte[] serialized) {
            this.serialized = serialized;
        }

        public void parse() {
            salt = new byte[Constants.SALT_LENGTH];
            iv = new byte[Constants.GCM_IV_LENGTH];
            ciphertext = new byte[serialized.length - (Constants.SALT_LENGTH + Constants.GCM_IV_LENGTH + Constants.VERSION_LENGTH)];
            ByteBuffer bb = ByteBuffer.wrap(serialized);
            version = bb.getInt();
            bb.get(salt);
            bb.get(iv);
            bb.get(ciphertext);
        }

        public byte[] concatenate() {
            serialized = new byte[Constants.VERSION_LENGTH + Constants.SALT_LENGTH + Constants.GCM_IV_LENGTH + ciphertext.length];
            ByteBuffer bb = ByteBuffer.wrap(serialized);
            bb.putInt(version);
            bb.put(salt);
            bb.put(iv);
            bb.put(ciphertext);
            serialized = bb.array();
            return serialized;
        }

    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroy()");
        Wiper.wipe((SecretKeySpec)mSecretKey);
    }

}
