package info.guardianproject.cacheword;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.support.v4.content.LocalBroadcastManager;

public class CacheWordService extends Service {

	private final static String TAG = "CacheWordService";

	private final IBinder mBinder = new CacheWordBinder();

	private CachedSecrets mSecrets;
	private int mTimeoutMinutes = Constants.DEFAULT_TIMEOUT_MINUTES;

	private PendingIntent mTimeoutIntent;
	private Intent mBroadcastIntent = new Intent(Constants.INTENT_NEW_SECRETS);

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

	// / private methods
	// ////////////////////////////////////

	private void handleNewSecrets() {

		if( !SecretsManager.isInitialized(this) ) {
			if( !initializeSecretStorage() ) {
				//TODO(abel): how to handle this error condition?
				Log.e(TAG, "failed to initialize secret storage");
				return;
			}
		}

		int timeoutMinutes = getTimeoutMinutes();
		boolean timeoutEnabled = timeoutMinutes > 0;

		if (timeoutEnabled) {
			startTimeout(timeoutMinutes * 60 * 1000);
			goForeground();
		}

		LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);
	}

	private boolean initializeSecretStorage() {
		//TODO(abel): here will encrypt the secrets and store them securely, but for now we just write a dummy value

		SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS, Constants.SHARED_PREFS_PRIVATE_MODE);
		Editor editor = prefs.edit();
		editor.putString(Constants.SHARED_PREFS_SECRETS, "dummy value");
		editor.putBoolean(Constants.SHARED_PREFS_INITIALIZED, true);
		return editor.commit();
	}

	private void expirePassphrase() {
		stopForeground(true);

		synchronized (this) {
			mSecrets = null;
		}
		LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);
	}

	/**
	 * @param interval
	 *            in milliseconds
	 */
	private void startTimeout(long interval) {
		Log.d(TAG, "starting timeout: " + interval);

		if (mTimeoutIntent == null) {
			Intent passExpiredIntent = new Intent(
					Constants.INTENT_PASS_EXPIRED, null, this,
					CacheWordService.class);
			mTimeoutIntent = PendingIntent.getService(this, 0,
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
		Intent notificationIntent = new Intent(Constants.INTENT_PASS_EXPIRED,
				null, this, CacheWordService.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this,
				getText(R.string.cacheword_notification_cached_title),
				getText(R.string.cacheword_notification_cached_message),
				pendingIntent);

		startForeground(Constants.SERVICE_FOREGROUND_ID, notification);
	}

	public class CacheWordBinder extends Binder implements ICacheWordBinder {

		public CacheWordService getService() {
			Log.d("CacheWordBinder", "giving service");
			return CacheWordService.this;
		}
	}

}