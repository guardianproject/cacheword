
package info.guardianproject.cacheword.test;

import android.test.AndroidTestCase;
import android.util.Log;

import info.guardianproject.cacheword.Constants;
import info.guardianproject.cacheword.PassphraseSecrets;
import info.guardianproject.cacheword.PassphraseSecrets.SerializedSecretsV1;
import info.guardianproject.cacheword.SecretsManager;

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
        SerializedSecretsV1 ss_first = new SerializedSecretsV1(original_ciphertext);
        SerializedSecretsV1 ss_second = new SerializedSecretsV1(new_ciphertext);

        ss_first.parse();
        ss_second.parse();

        assertEquals(ss_first.version, ss_second.version);
        assertFalse(Arrays.equals(ss_first.iv, ss_second.iv));
        assertFalse(Arrays.equals(ss_first.salt, ss_second.salt));
        assertFalse(Arrays.equals(ss_first.ciphertext, ss_second.ciphertext));

    }

}
