
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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Observable;
import java.util.Observer;

public class CacheWordService extends Service implements Observer {

    private final static String TAG = "CacheWordService";

    private final IBinder mBinder = new CacheWordBinder();

    private ICachedSecrets mSecrets = null;

    private Notification mNotification;
    private PendingIntent mTimeoutIntent;
    private Intent mBroadcastIntent = new Intent(Constants.INTENT_NEW_SECRETS);

    private int mSubscriberCount = 0;
    private boolean mIsForegrounded = false;

    private CacheWordSettings mSettings = null;

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
            Log.d(TAG, "onStart: LOCK COMMAND received..locking");
            lock();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mSettings = new CacheWordSettings(this);
        mSettings.addObserver(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved()");
        if (!mIsForegrounded) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(Constants.SERVICE_BACKGROUND_ID);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSecrets != null) {
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

        handleNewSecrets(true);
    }

    public CacheWordSettings settings() {
        return mSettings;
    }

    public void setSettings(CacheWordSettings settings) {
        mSettings.updateWith(settings);
    }

    public synchronized boolean isLocked() {
        return mSecrets == null;
    }

    public void lock() {
        Log.d(TAG, "lock");

        synchronized (this) {
            if (mSecrets != null) {
                mSecrets.destroy();
                mSecrets = null;
            }
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);

        if (mIsForegrounded) {
            stopForeground(true);
            mIsForegrounded = false;
        }
        stopSelf();
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

    private void handleNewSecrets(boolean notify) {
        if (!SecretsManager.isInitialized(this)) {
            return;
        }
        if (mNotification != null) {
            stopForeground(true);
            startForeground(Constants.SERVICE_FOREGROUND_ID, mNotification);
            mIsForegrounded = true;
        } else {
            if (mIsForegrounded) {
                stopForeground(true);
                mIsForegrounded = false;
            }
        }
        resetTimeout();
        if (notify)
            LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);
    }

    private void resetTimeout() {
        int timeoutSeconds = mSettings.getTimeoutSeconds();
        boolean timeoutEnabled = timeoutSeconds >= 0;

        Log.d(TAG, "timeout enabled: " + timeoutEnabled + ", seconds=" + timeoutSeconds);
        Log.d(TAG, "mSubscriberCount: " + mSubscriberCount);

        if (timeoutEnabled && mSubscriberCount == 0) {
            startTimeout(timeoutSeconds * 1000);
        } else {
            Log.d(TAG, "disabled timeout alarm");
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(mTimeoutIntent);
        }
    }

    /**
     * @param milliseconds timeout interval in milliseconds
     */
    private void startTimeout(long milliseconds) {
        if (milliseconds <= 0) {
            Log.d(TAG, "immediate timeout");
            lock();
            return;
        }
        Log.d(TAG, "starting timeout: " + milliseconds);

        if (mTimeoutIntent == null) {
            Intent passExpiredIntent = CacheWordService.getBlankServiceIntent(this);
            passExpiredIntent.setAction(Constants.INTENT_PASS_EXPIRED);
            mTimeoutIntent = PendingIntent.getService(getApplicationContext(), 0,
                    passExpiredIntent, 0);
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + milliseconds, mTimeoutIntent);

    }

    public void setNotification(Notification notification) {
        mNotification = notification;
    }

    public class CacheWordBinder extends Binder implements ICacheWordBinder {

        @Override
        public CacheWordService getService() {
            Log.d("CacheWordBinder", "giving service");
            return CacheWordService.this;
        }
    }

    /**
     * Create a blank intent to start an instance of {@link CacheWordService}.
     * It is called "blank" because only the Component field is set.
     *
     * @param context
     * @return an Intent used to send a message to {@link CacheWordService}
     */
    static public Intent getBlankServiceIntent(Context context) {
        Intent i = new Intent();
        i.setClassName(context.getApplicationContext(), Constants.SERVICE_CLASS_NAME);
        return i;
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable == mSettings) {
            resetTimeout();
            // update backgrounding & notification without alerting the
            // subscribers
            handleNewSecrets(false);
        }
    }
}
