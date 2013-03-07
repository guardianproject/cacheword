package info.guardianproject.cacheword;

import android.content.Context;
import android.content.SharedPreferences;

public class SecretsManager {

	public static boolean isInitialized(Context ctx) {
		return getPrefs(ctx).getBoolean(Constants.SHARED_PREFS_INITIALIZED, false);
	}

	private static SharedPreferences getPrefs(Context ctx) {
		return ctx.getSharedPreferences(Constants.SHARED_PREFS, Constants.SHARED_PREFS_PRIVATE_MODE);
	}

}
