
package info.guardianproject.cacheword;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CacheWordService extends Service {

    private final static String TAG = "CacheWordService";

    private final IBinder mBinder = new CacheWordBinder();

    private ICachedSecrets mSecrets;
    private int mTimeoutMinutes = Constants.DEFAULT_TIMEOUT_MINUTES;

    private PendingIntent mTimeoutIntent;
    private Intent mBroadcastIntent = new Intent(Constants.INTENT_NEW_SECRETS);

    private int mSubscriberCount = 0;
    private boolean mIsForegrounded = false;

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
    public void onDestroy() {
        super.onDestroy();
        if( mSecrets != null )
            mSecrets.destroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // note: this method is called when ALL clients
        // have unbound, and not per-client.
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

    public synchronized int getTimeoutMinutes() {
        return mTimeoutMinutes;
    }

    public synchronized void setTimeoutMinutes(int minutes) {
        if(minutes != mTimeoutMinutes) {
            mTimeoutMinutes = minutes;
            resetTimeout();
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

    private Notification buildNotification() {

        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setSmallIcon(R.drawable.ic_menu_key);
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

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(Constants.SERVICE_BACKGROUND_ID, buildNotification());

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

    private boolean shouldForeground() {
        return getSharedPreferences(Constants.SHARED_PREFS, 0).getBoolean(Constants.SHARED_PREFS_FOREGROUND, false);
    }
}
