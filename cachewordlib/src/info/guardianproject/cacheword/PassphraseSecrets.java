
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

    public static PassphraseSecrets initializeSecrets(Context ctx, char[] passphrase) {
        try {
            byte[] salt = generateSalt(Constants.SALT_LENGTH);
            byte[] iv = generateIv(Constants.GCM_IV_LENGTH);

            SecretKey passphraseKey = hashPassphrase(passphrase, salt);
            SecretKey secretKey = generateSecretKey();
            byte[] encryptedSecretKey = encryptSecretKey(passphraseKey, iv, secretKey.getEncoded());

            SerializedSecrets ss = new SerializedSecrets(salt, iv, encryptedSecretKey);
            byte[] preparedSecret = ss.concatenate();

            boolean saved = SecretsManager.saveBytes(ctx, Constants.SHARED_PREFS_SECRETS, preparedSecret);
            SecretsManager.setInitialized(ctx, saved);

            return new PassphraseSecrets(secretKey);
        } catch (GeneralSecurityException e ) {
            Log.e(TAG, "initializeSecrets failed: " +e.getClass().getName() + " : " + e.getMessage());
            return null;
        }
    }

    public static PassphraseSecrets fetchSecrets(Context ctx, char[] passphrase)
            throws GeneralSecurityException {
        byte[] preparedSecret = SecretsManager.getBytes(ctx, Constants.SHARED_PREFS_SECRETS);

        SerializedSecrets ss = new SerializedSecrets(preparedSecret);
        ss.parse();
        byte[] salt = ss.salt;
        byte[] iv = ss.iv;
        byte[] ciphertext = ss.ciphertext;

        SecretKey passphraseKey = hashPassphrase(passphrase, salt);
        byte[] rawSecretKey = decryptSecretKey(passphraseKey, iv, ciphertext);


        return new PassphraseSecrets(rawSecretKey);
    }

    // used by initialization and verification routines

    private static SecretKey hashPassphrase(char[] passwd, byte[] salt)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(passwd, salt, Constants.PBKDF2_ITER_COUNT, Constants.PBKDF2_KEY_LEN);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    // verification routines: used to unlock secrets

    private static byte[] decryptSecretKey(SecretKey passphraseKey, byte[] iv, byte[] ciphertext)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, passphraseKey, new IvParameterSpec(iv));

        return cipher.doFinal(ciphertext);
    }

    // initialization routines: creates secrets


    private static byte[] encryptSecretKey(SecretKey passphraseKey, byte[] iv, byte[] data)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, passphraseKey, new IvParameterSpec(iv));

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
    private static class SerializedSecrets {
        public byte[] salt;
        public byte[] iv;
        public byte[] ciphertext;
        public byte[] serialized;

        public SerializedSecrets(byte[] salt, byte[] iv, byte[] ciphertext) {
            this.salt = salt;
            this.iv = iv;
            this.ciphertext = ciphertext;
        }

        public SerializedSecrets(byte[] serialized) {
            this.serialized = serialized;
        }

        public void parse() {
            salt = new byte[Constants.SALT_LENGTH];
            iv = new byte[Constants.GCM_IV_LENGTH];
            ciphertext = new byte[serialized.length - Constants.SALT_LENGTH - Constants.GCM_IV_LENGTH];
            ByteBuffer bb = ByteBuffer.wrap(serialized);
            bb.get(salt);
            bb.get(iv);
            bb.get(ciphertext);
        }

        public byte[] concatenate() {
            serialized = new byte[salt.length + iv.length + ciphertext.length];
            ByteBuffer bb = ByteBuffer.wrap(serialized);
            bb.put(salt);
            bb.put(iv);
            bb.put(ciphertext);
            serialized = bb.array();
            return serialized;
        }

    }

}
