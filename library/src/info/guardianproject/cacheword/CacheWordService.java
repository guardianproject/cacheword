
package info.guardianproject.cacheword;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CacheWordService extends Service {

    private final static String TAG = "CacheWordService";

    private final IBinder mBinder = new CacheWordBinder();

    private CachedSecrets mSecrets;
    private int mTimeoutMinutes = Constants.DEFAULT_TIMEOUT_MINUTES;

    private PendingIntent mTimeoutIntent;
    private Intent mBroadcastIntent = new Intent(Constants.INTENT_NEW_SECRETS);

    private int mSubscriberCount = 0;

    @Override
    public void onStart(Intent intent, int startId) {
        if (intent == null)
            return;

        String action = intent.getAction();
        if (action == null)
            return;

        Log.d(TAG, "started with intent " + action);

        if (action.equals(Constants.INTENT_PASS_EXPIRED)) {
            expirePassphrase();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // API for Clients
    // //////////////////////////////////////

    public synchronized CachedSecrets getCachedSecrets() {
        return mSecrets;
    }

    public synchronized void setCachedSecrets(CachedSecrets secrets) {
        Log.d(TAG, "setCachedSecrets()");
        mSecrets = secrets;

        handleNewSecrets();
    }

    public synchronized int getTimeoutMinutes() {
        return mTimeoutMinutes;
    }

    public synchronized void setTimeoutMinutes(int minutes) {
        mTimeoutMinutes = minutes;
    }

    public synchronized boolean isLocked() {
        return mSecrets == null;
    }

    public void manuallyLock() {
        expirePassphrase();
    }

    public synchronized void attachSubscriber() {
        mSubscriberCount++;
        resetTimeout();
    }

    public synchronized void detachSubscriber() {
        mSubscriberCount--;
        resetTimeout();
    }

    // / private methods
    // ////////////////////////////////////

    private void handleNewSecrets() {

        if (!SecretsManager.isInitialized(this)) {
            if (!initializeSecretStorage()) {
                // TODO(abel): how to handle this error condition?
                Log.e(TAG, "failed to initialize secret storage");
                return;
            }
        }

        goForeground();
        resetTimeout();
        LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);
    }

    private boolean initializeSecretStorage() {
        // TODO(abel): here will encrypt the secrets and store them securely,
        // but for now we just write a dummy value

        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS,
                Constants.SHARED_PREFS_PRIVATE_MODE);
        Editor editor = prefs.edit();
        editor.putString(Constants.SHARED_PREFS_SECRETS, "dummy value");
        editor.putBoolean(Constants.SHARED_PREFS_INITIALIZED, true);
        return editor.commit();
    }

    private void expirePassphrase() {
        Log.d(TAG, "expirePassphrase");
        stopForeground(true);

        synchronized (this) {
            mSecrets = null;
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);
    }

    private void resetTimeout() {
        int timeoutMinutes = getTimeoutMinutes();
        boolean timeoutEnabled = timeoutMinutes > 0;

        Log.d(TAG, "timeout enabled: " + timeoutEnabled);
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

    private void goForeground() {
        Log.d(TAG, "goForeground()");

        Notification notification = new Notification(R.drawable.ic_menu_key,
                getText(R.string.cacheword_notification_cached),
                System.currentTimeMillis());
        Intent notificationIntent = CacheWordService.getBlankServiceIntent(getApplicationContext());
        notificationIntent.setAction(Constants.INTENT_PASS_EXPIRED);

        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0,
                notificationIntent, 0);
        notification.setLatestEventInfo(this,
                getText(R.string.cacheword_notification_cached_title),
                getText(R.string.cacheword_notification_cached_message),
                pendingIntent);

        stopForeground(true);
        startForeground(Constants.SERVICE_FOREGROUND_ID, notification);
    }

    public class CacheWordBinder extends Binder implements ICacheWordBinder {

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

}
