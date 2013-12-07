
package info.guardianproject.cacheword.test;

import android.test.AndroidTestCase;
import android.util.Log;

import info.guardianproject.cacheword.PassphraseSecrets;
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
        char[] pass = "hunter2".toCharArray();

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

}
