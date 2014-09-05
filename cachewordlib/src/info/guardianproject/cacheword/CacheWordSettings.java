
package info.guardianproject.cacheword;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.util.Observable;

public class CacheWordSettings extends Observable {

    public static final String TAG = "CacheWordSettings";
    private Context mContext;

    /**
     * Timeout: How long to wait before automatically locking and wiping the
     * secrets after all your activities are no longer visible This is the
     * default setting, and can be changed at runtime via a preference A value
     * of 0 represents instant timeout A value < 0 represents no timeout (or
     * infinite timeout)
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    /**
     * Creates a CacheWordSettings object with all the default settings
     *
     * @param context your app's context, used to read SharedPreferences
     */
    public CacheWordSettings(Context context) {
        mContext = context;
        loadDefaults();
    }

    /**
     * Load the default settings from XML and save them in Shared Prefs
     */
    private void loadDefaults() {
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.SHARED_PREFS, 0);
        Editor ed = prefs.edit();

        if (!prefs.contains(Constants.SHARED_PREFS_TIMEOUT_SECONDS)) {
            // timeout
            ed.putInt(Constants.SHARED_PREFS_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
        }
        ed.commit();
    }

    /**
     * Retrieve the current timeout setting The default value can be changed by
     * copying res/values/cacheword.xml to your project and editing it. The
     * value is stored in SharedPreferences, so it will persist.
     *
     * @return the timeout in seconds
     */
    public synchronized int getTimeoutSeconds() {
        int timeout = mContext.getSharedPreferences(Constants.SHARED_PREFS, 0).getInt(
                Constants.SHARED_PREFS_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
        return timeout;
    }

    /**
     * Sets the timeout, if a timeout is running it will be restarted with the
     * new timeout value. The default value can be changed by copying
     * res/values/cacheword.xml to your project and editing it. The value is
     * stored in SharedPreferences, so it will persist.
     *
     * @param seconds
     */
    public synchronized void setTimeoutSeconds(int seconds) {
        if (seconds >= 0 && seconds != getTimeoutSeconds()) {
            Editor ed = mContext.getSharedPreferences(Constants.SHARED_PREFS, 0).edit();
            ed.putInt(Constants.SHARED_PREFS_TIMEOUT_SECONDS, seconds);
            ed.commit();
            Log.d(TAG, "setTimeoutSeconds() seconds=" + seconds);
            notifyObservers();
        }
    }

    public void updateWith(CacheWordSettings other) {
        setTimeoutSeconds(other.getTimeoutSeconds());
    }

}
