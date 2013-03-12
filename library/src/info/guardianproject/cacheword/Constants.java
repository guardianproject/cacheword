package info.guardianproject.cacheword;

public class Constants {

	// Service class name
	public static final String SERVICE_CLASS_NAME = "info.guardianproject.cacheword.CacheWordService";

	// Intents
	public static final String INTENT_PASS_EXPIRED = "info.guardianproject.cacheword.PASS_EXPIRED";
	public static final String INTENT_NEW_SECRETS = "info.guardianproject.cacheword.NEW_SECRETS";

	// Values
	public static final String SHARED_PREFS = "info.guardianproject.cacheword.prefs";
	public static final int  SHARED_PREFS_PRIVATE_MODE = 0;
	public static final String SHARED_PREFS_INITIALIZED = "initialized";
	public static final String SHARED_PREFS_SECRETS = "encrypted_secrets";

	public static final int DEFAULT_TIMEOUT_MINUTES = 5;
	public static final int SERVICE_FOREGROUND_ID = 81231;

	public static final int STATE_UNKNOWN = -1;
	public static final int STATE_UNINITIALIZED = 0;
	public static final int STATE_LOCKED = 1;
	public static final int STATE_UNLOCKED = 2;


}
