
package info.guardianproject.cacheword;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CacheWordService extends Service {

    private final static String TAG = "CacheWordService";

    private final IBinder mBinder = new CacheWordBinder();

    private ICachedSecrets mSecrets = null;

    private PendingIntent mTimeoutIntent;
    private Intent mBroadcastIntent = new Intent(Constants.INTENT_NEW_SECRETS);

    private int mSubscriberCount = 0;
    private boolean mIsForegrounded = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(TAG, "onStart: null intent");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (action == null) {
            Log.d(TAG, "onStart: null action");
            return START_NOT_STICKY;
        }

        Log.d(TAG, "onStart: with intent " + action);

        if (action.equals(Constants.INTENT_PASS_EXPIRED)) {
            expirePassphrase();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved()");
        if(!mIsForegrounded) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(Constants.SERVICE_BACKGROUND_ID);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if( mSecrets != null ) {
            Log.d(TAG, "onDestroy() killed secrets");
            mSecrets.destroy();
            mSecrets = null;
        } else {
            Log.d(TAG, "onDestroy() secrets already null");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // note: this method is called when ALL clients
        // have unbound, and not per-client.
        resetTimeout();
        return super.onUnbind(intent);
    }
    @Override
    public IBinder onBind(Intent intent) {
        // note: this method is called on the first binding
        // not per-client
        return mBinder;
    }

    // API for Clients
    // //////////////////////////////////////

    public synchronized ICachedSecrets getCachedSecrets() {
        return mSecrets;
    }

    public synchronized void setCachedSecrets(ICachedSecrets secrets) {
        Log.d(TAG, "setCachedSecrets()");
        mSecrets = secrets;

        handleNewSecrets();
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
        int timeout = getSharedPreferences(Constants.SHARED_PREFS, 0).getInt(Constants.SHARED_PREFS_TIMEOUT, -255);
        if( timeout == -255)
            return getResources().getInteger(R.integer.cacheword_timeout_minutes_default);
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
            Editor ed = getSharedPreferences(Constants.SHARED_PREFS, 0).edit();
            ed.putInt(Constants.SHARED_PREFS_TIMEOUT, minutes);
            ed.commit();
            resetTimeout();
            Log.d(TAG, "setTimeoutMinutes() minutes=" + minutes);
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
        boolean use_notification = getResources().getBoolean(R.bool.cacheword_use_notification_default);
        use_notification = getSharedPreferences(Constants.SHARED_PREFS, 0).getBoolean(Constants.SHARED_PREFS_USE_NOTIFICATION, use_notification);
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
            Editor ed = getSharedPreferences(Constants.SHARED_PREFS, 0).edit();
            ed.putBoolean(Constants.SHARED_PREFS_USE_NOTIFICATION, enabled);
            ed.commit();
            resetTimeout();
            Log.d(TAG, "setEnableNotification() enabled=" + enabled);
        }
    }

    public synchronized boolean isLocked() {
        return mSecrets == null;
    }

    public void manuallyLock() {
        expirePassphrase();
    }

    public synchronized void attachSubscriber() {
        mSubscriberCount++;
        Log.d(TAG, "attachSubscriber(): " + mSubscriberCount);
        resetTimeout();
    }

    public synchronized void detachSubscriber() {
        mSubscriberCount--;
        Log.d(TAG, "detachSubscriber(): " + mSubscriberCount);
        resetTimeout();
    }

    // / private methods
    // ////////////////////////////////////

    private void handleNewSecrets() {

        if (!SecretsManager.isInitialized(this)) {
            return;
        }

        if( shouldForeground() )
            goForeground();
        else
            goBackground();
        resetTimeout();
        LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);
    }

    private void expirePassphrase() {
        Log.d(TAG, "expirePassphrase");

        synchronized (this) {
            if( mSecrets != null ) {
                mSecrets.destroy();
                mSecrets = null;
            }
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);

        if( mIsForegrounded ) {
            stopForeground(true);
            mIsForegrounded = false;
        } else {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(Constants.SERVICE_BACKGROUND_ID);
        }
        stopSelf();
    }

    private void resetTimeout() {
        int timeoutMinutes = getTimeoutMinutes();
        boolean timeoutEnabled = timeoutMinutes >= 0;

        Log.d(TAG, "timeout enabled: " + timeoutEnabled + ", minutes="+timeoutMinutes);
        Log.d(TAG, "mSubscriberCount: " + mSubscriberCount);

        if (timeoutEnabled && mSubscriberCount == 0) {
            startTimeout(timeoutMinutes * 60 * 1000);
        } else {
            Log.d(TAG, "disabled timeout alarm");
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(mTimeoutIntent);
        }
    }

    /**
     * @param interval in milliseconds
     */
    private void startTimeout(long interval) {
        if( interval <= 0 ) {
            Log.d(TAG, "immediate timeout");
            expirePassphrase();
            return;
        }
        Log.d(TAG, "starting timeout: " + interval);

        if (mTimeoutIntent == null) {
            Intent passExpiredIntent = CacheWordService
                    .getBlankServiceIntent(getApplicationContext());
            passExpiredIntent.setAction(Constants.INTENT_PASS_EXPIRED);
            mTimeoutIntent = PendingIntent.getService(getApplicationContext(), 0,
                    passExpiredIntent, 0);
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + interval, mTimeoutIntent);

    }

    private Notification buildNotification() {

        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setSmallIcon(R.drawable.cacheword_notification_icon);
        b.setContentTitle(getText(R.string.cacheword_notification_cached_title));
        b.setContentText(getText(R.string.cacheword_notification_cached_message));
        b.setTicker(getText(R.string.cacheword_notification_cached));
        b.setDefaults(Notification.DEFAULT_VIBRATE);
        b.setWhen(System.currentTimeMillis());
        b.setOngoing(true);
        Intent notificationIntent = CacheWordService.getBlankServiceIntent(getApplicationContext());
        notificationIntent.setAction(Constants.INTENT_PASS_EXPIRED);
        b.setContentIntent(PendingIntent.getService(getApplicationContext(), 0,
            notificationIntent, 0));

        return b.build();
    }

    private void goForeground() {
        Log.d(TAG, "goForeground()");

        stopForeground(true);
        startForeground(Constants.SERVICE_FOREGROUND_ID, buildNotification());
        mIsForegrounded = true;
    }

    private void goBackground() {
        Log.d(TAG, "goBackground()");

        if(mIsForegrounded) {
            stopForeground(true);
            mIsForegrounded = false;
        }

        if( getNotificationEnabled() ) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(Constants.SERVICE_BACKGROUND_ID, buildNotification());
        }

    }

    public class CacheWordBinder extends Binder implements ICacheWordBinder {

        @Override
        public CacheWordService getService() {
            Log.d("CacheWordBinder", "giving service");
            return CacheWordService.this;
        }
    }

    /**
     * Create a blank intent to start the CachewordService Blank means only the
     * Component field is set
     *
     * @param applicationContext
     */
    static public Intent getBlankServiceIntent(Context applicationContext) {
        Intent i = new Intent();
        i.setClassName(applicationContext, Constants.SERVICE_CLASS_NAME);
        return i;
    }

    private boolean shouldForeground() {
        return getSharedPreferences(Constants.SHARED_PREFS, 0).getBoolean(Constants.SHARED_PREFS_FOREGROUND, false);
    }
}
