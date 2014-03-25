
package info.guardianproject.cacheword.test;

import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

import info.guardianproject.cacheword.Constants;
import info.guardianproject.cacheword.PassphraseSecrets;
import info.guardianproject.cacheword.PassphraseSecretsImpl;
import info.guardianproject.cacheword.SecretsManager;
import info.guardianproject.cacheword.SerializedSecretsLoader;
import info.guardianproject.cacheword.SerializedSecretsV0;
import info.guardianproject.cacheword.SerializedSecretsV1;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class PassphraseSecretsTest extends AndroidTestCase {
    private final static String TAG = "PassphraseSecretsCompatTest";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SecretsManager.setInitialized(getContext(), false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testInitializeAndFetch() {
        PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();
        String passphrase_str = "hunter2";
        char[] pass = passphrase_str.toCharArray();

        PassphraseSecrets original_secrets = PassphraseSecrets
                .initializeSecrets(getContext(), pass);
        assertNotNull(original_secrets);
        assertNotNull(original_secrets.getSecretKey());
        assertFalse(Arrays.equals(pass, passphrase_str.toCharArray()));
        assertTrue(SecretsManager.isInitialized(getContext()));

        Log.d(TAG, "key fmt: " + original_secrets.getSecretKey().getFormat());

        pass = passphrase_str.toCharArray();
        PassphraseSecrets fetched_secrets = null;
        try {
            fetched_secrets = PassphraseSecrets.fetchSecrets(getContext(), pass);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertFalse(Arrays.equals(pass, passphrase_str.toCharArray()));
        assertTrue(Arrays.equals(fetched_secrets.getSecretKey().getEncoded(),
                original_secrets.getSecretKey().getEncoded()));
    }

    public void testChangePassword() {
        String passphrase_str = "hunter2";
        String passphrase_str_new = "purplepipers";

        char[] pass = passphrase_str.toCharArray();
        PassphraseSecrets original_secrets = PassphraseSecrets
                .initializeSecrets(getContext(), pass);

        assertNotNull(original_secrets);

        assertTrue(SecretsManager.isInitialized(getContext()));

        byte[] original_ciphertext  = SecretsManager.getBytes(getContext(), Constants.SHARED_PREFS_SECRETS);
        assertNotNull(original_ciphertext);

        char[] pass_new = passphrase_str_new.toCharArray();
        PassphraseSecrets new_secrets = PassphraseSecrets.changePassphrase(getContext(), original_secrets, pass_new);

        assertNotNull(new_secrets);

        // The underlying AES secret key should still be the same
        assertTrue(Arrays.equals(new_secrets.getSecretKey().getEncoded(),
                original_secrets.getSecretKey().getEncoded()));

        // fetching with the old passphrase should fail
        try {
            pass = passphrase_str.toCharArray();
            PassphraseSecrets.fetchSecrets(getContext(), pass);
            fail("fetchSecrets should fail with the old passphrase");
        } catch (GeneralSecurityException e) {
            // pass
        }

        // fetch the secrets from disk and verify the AES secret key is still the same
        PassphraseSecrets fetched_secrets = null;
        try {
            pass_new = passphrase_str_new.toCharArray();
            fetched_secrets = PassphraseSecrets.fetchSecrets(getContext(), pass_new);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertTrue(Arrays.equals(fetched_secrets.getSecretKey().getEncoded(),
                original_secrets.getSecretKey().getEncoded()));


        // but the ciphertext should be different
        byte[] new_ciphertext  = SecretsManager.getBytes(getContext(), Constants.SHARED_PREFS_SECRETS);
        assertFalse(Arrays.equals(new_ciphertext, original_ciphertext));

        // verify this in depth
        SerializedSecretsV0 ss_first = new SerializedSecretsV0(original_ciphertext);
        SerializedSecretsV0 ss_second = new SerializedSecretsV0(new_ciphertext);

        ss_first.parse();
        ss_second.parse();

        assertEquals(ss_first.version, ss_second.version);
        assertFalse(Arrays.equals(ss_first.iv, ss_second.iv));
        assertFalse(Arrays.equals(ss_first.salt, ss_second.salt));
        assertFalse(Arrays.equals(ss_first.ciphertext, ss_second.ciphertext));

    }

    public void testVersion0Migration() {
        // a string exported from version 0 with pass 'purplepipers'
        String encoded = "AAAAABGEgBW8ATWLekRtu1ODaQswXZ2Tr0hfzl9rSz+kRAA8fu5pjKXPaKcT18zS7xPKV4z4DG5W49wV6bPaGTdP7Co3srPmEPPcAATECMY=";

        byte[] decoded  = Base64.decode(encoded, Base64.DEFAULT);

        SerializedSecretsV0 ss0 = new SerializedSecretsV0(decoded);
        ss0.parse();
        assertEquals(Constants.VERSION_ZERO, ss0.version);

        SerializedSecretsLoader loader = new SerializedSecretsLoader();
        assertEquals(Constants.VERSION_ZERO, loader.getVersion(decoded));

        SerializedSecretsV1 ss1 = loader.loadSecrets(decoded);
        assertEquals(100, ss1.pbkdf_iter_count);

        SecretsManager.saveBytes(getContext(), Constants.SHARED_PREFS_SECRETS, ss1.concatenate());
        PassphraseSecrets fetched_secrets = null;

        try {
            char[] pass_new = "purplepipers".toCharArray();
            fetched_secrets = PassphraseSecrets.fetchSecrets(getContext(), pass_new);
            assertNotNull(fetched_secrets);
            assertNotNull(fetched_secrets.getSecretKey());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
