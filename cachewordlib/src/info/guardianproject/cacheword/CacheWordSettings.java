package info.guardianproject.cacheword;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.util.Observable;

public class CacheWordSettings extends Observable {

    public static final String TAG = "CacheWordSettings";
    private Context mContext;
    private PendingIntent mDefaultNotificationIntent = null;

    /**
     * Creates a CacheWordSettings object with all the default settings
     * @param context your app's context, used to read SharedPreferences
     */
    public CacheWordSettings(Context context) {
        mContext = context;
    }

    /**
     * Retrieve the current timeout setting
     * The default value can be changed by copying res/values/cacheword.xml to your project
     * and editing it.
     *
     * The value is stored in  SharedPreferences, so it will persist.
     * @return the timeout in minutes
     */
    public synchronized int getTimeoutMinutes() {
        int defTimeout = mContext.getResources().getInteger(R.integer.cacheword_timeout_minutes_default);
        int timeout = mContext.getSharedPreferences(Constants.SHARED_PREFS, 0).getInt(Constants.SHARED_PREFS_TIMEOUT, defTimeout);
        return timeout;
    }

    /**
     * Sets the timeout, if a timeout is running it will be restarted with the
     * new timeout value.
     * The default value can be changed by copying res/values/cacheword.xml to your project
     * and editing it.
     *
     * The value is stored in  SharedPreferences, so it will persist.
     * @param minutes
     */
    public synchronized void setTimeoutMinutes(int minutes) {
        if(minutes >= 0 && minutes != getTimeoutMinutes()) {
            Editor ed = mContext.getSharedPreferences(Constants.SHARED_PREFS, 0).edit();
            ed.putInt(Constants.SHARED_PREFS_TIMEOUT, minutes);
            ed.commit();
            Log.d(TAG, "setTimeoutMinutes() minutes=" + minutes);
            notifyObservers();
        }
    }

    /**
     * Retrieve whether the notification shown when CacheWord is unlocked
     * should vibrate device or not.
     * The default value can be changed by copying res/values/cacheword.xml to your project
     * and editing it.
     *
     * The value is stored in  SharedPreferences, so it will persist.
     * @return true if vibration is allowed, false otherwise
     */
    public synchronized boolean getVibrateSetting() {
        boolean defValue = mContext.getResources().getBoolean(R.bool.cacheword_vibrate_default);
        return mContext.getSharedPreferences(Constants.SHARED_PREFS, 0).getBoolean(Constants.SHARED_PREFS_VIBRATE, defValue);
    }

    /**
     * Set whether the notification shown when CacheWord is unlocked
     * should vibrate device or not.
     * The default value can be changed by copying res/values/cacheword.xml to your project
     * and editing it.
     *
     * The value is stored in  SharedPreferences, so it will persist.
     * @param vibrate
     */
    public synchronized void setVibrateSetting(boolean vibrate) {
        if(vibrate != getVibrateSetting()) {
            Editor ed = mContext.getSharedPreferences(Constants.SHARED_PREFS, 0).edit();
            ed.putBoolean(Constants.SHARED_PREFS_VIBRATE, vibrate);
            ed.commit();
            Log.d(TAG, "setVibrateSetting() vibrate = " + vibrate);
            notifyObservers();
        }
    }

    /**
     * Retrieve whether a notification is shown when CacheWord is unlocked
     * The default value can be changed by copying res/values/cacheword.xml to your project
     * and editing it.
     *
     * The value is stored in  SharedPreferences, so it will persist.
     * @return true if the notification is enabled
     */
    public synchronized boolean getNotificationEnabled() {
        boolean use_notification = mContext.getResources().getBoolean(R.bool.cacheword_use_notification_default);
        use_notification = mContext.getSharedPreferences(Constants.SHARED_PREFS, 0).getBoolean(Constants.SHARED_PREFS_USE_NOTIFICATION, use_notification);
        return use_notification;
    }

    /**
     * Set whether to show a notification when CacheWord is unlocked.
     * The default value can be changed by copying res/values/cacheword.xml to your project
     * and editing it.
     *
     * The value is stored in  SharedPreferences, so it will persist.
     * @param enabled
     */
    public synchronized void setEnableNotification(boolean enabled) {
        if(enabled!= getNotificationEnabled()) {
            Editor ed = mContext.getSharedPreferences(Constants.SHARED_PREFS, 0).edit();
            ed.putBoolean(Constants.SHARED_PREFS_USE_NOTIFICATION, enabled);
            ed.commit();
            Log.d(TAG, "setEnableNotification() enabled=" + enabled);
            notifyObservers();
        }
    }


    /**
     * Sets the intent that is executed when the user taps the unlocked notification. The default
     * behavior is to lock the application. Passing null will revert to the default behavior.
     * @param intent the pending intent to execute
     */
    public synchronized void setNotificationIntent(PendingIntent intent) {
        // only change the intent if, it is actually changing.
        // which is when the current one is null, and the new one isn't, or
        // when the new intent != the old intent
        if( (mDefaultNotificationIntent == null && intent != null) || (mDefaultNotificationIntent != null && !mDefaultNotificationIntent.equals(intent)) ) {
            mDefaultNotificationIntent = intent;
            notifyObservers();
        }
    }


    /**
     * The default intent is null, which causes the application to lock when the user taps the notification.
     * @return
     */
    public synchronized PendingIntent getNotificationIntent() {
        return mDefaultNotificationIntent;
    }

    public void updateWith(CacheWordSettings other) {
        setTimeoutMinutes(other.getTimeoutMinutes());
        setVibrateSetting(other.getVibrateSetting());
        setEnableNotification(other.getNotificationEnabled());
        setNotificationIntent(other.getNotificationIntent());
    }

}
