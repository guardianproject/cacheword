
package info.guardianproject.cacheword;

public class Constants {

    // Service class name
    public static final String SERVICE_CLASS_NAME = "info.guardianproject.cacheword.CacheWordService";

    // Intents
    public static final String INTENT_PASS_EXPIRED = "info.guardianproject.cacheword.PASS_EXPIRED";
    public static final String INTENT_NEW_SECRETS = "info.guardianproject.cacheword.NEW_SECRETS";

    // Values
    public static final String SHARED_PREFS = "info.guardianproject.cacheword.prefs";
    public static final int SHARED_PREFS_PRIVATE_MODE = 0;
    public static final String SHARED_PREFS_INITIALIZED = "initialized";
    public static final String SHARED_PREFS_SECRETS = "encrypted_secrets";
    public static final String SHARED_PREFS_FOREGROUND = "foreground";

    public static final int DEFAULT_TIMEOUT_MINUTES = 5;
    public static final int SERVICE_FOREGROUND_ID = 81231;
    public static final int SERVICE_BACKGROUND_ID = 13218;

    public static final int STATE_UNKNOWN = -1;
    public static final int STATE_UNINITIALIZED = 0;
    public static final int STATE_LOCKED = 1;
    public static final int STATE_UNLOCKED = 2;

    // Crypto vars

    public static final int SALT_LENGTH = 16;
    public static final int AES_KEY_LENGTH = 256;
    public static final int GCM_IV_LENGTH = 12; // 96 bits
    public static final int PBKDF2_ITER_COUNT = 100;
    public static final int PBKDF2_KEY_LEN = 128;

}
