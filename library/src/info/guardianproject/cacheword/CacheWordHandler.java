package info.guardianproject.cacheword;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CacheWordHandler {
	private static final String TAG = "CacheWordHandler";

	private Context mContext;
	private CacheWordService mCacheWordService;
	private CacheWordSubscriber mSubscriber;

	public CacheWordHandler(Context context) {
		mContext = context;

		try {
			mSubscriber = (CacheWordSubscriber) context;
		} catch( ClassCastException e ) {
			throw new IllegalArgumentException("CacheWordHandler passed invalid Activity. Expects class that implements CacheWordSubscriber");
		}

		LocalBroadcastManager.getInstance(mContext).registerReceiver(mCacheWordReceiver, new IntentFilter(Constants.INTENT_NEW_SECRETS));
	}

	public void onResume() {
		Intent cacheWordIntent = new Intent();
		cacheWordIntent.setClassName(mContext.getApplicationContext(), "info.guardianproject.cacheword.CacheWordService");
		mContext.bindService(cacheWordIntent, mCacheWordServiceConnection, Context.BIND_AUTO_CREATE);
	}

	public void onPause() {
		if(mCacheWordService != null) {
			mContext.unbindService(mCacheWordServiceConnection);
		}
	}

	public CachedSecrets getCachedSecrets() {
		if( !isCacheWordConnected() ) return null;

		return mCacheWordService.getCachedSecrets();
	}

	public void setCachedSecrets(CachedSecrets secrets) {
		if( !isCacheWordConnected() ) return;

		mCacheWordService.setCachedSecrets(secrets);
	}

	public void manuallyLock() {
		if( !isPrepared() ) return;
		mCacheWordService.manuallyLock();
	}

	public boolean isLocked() {
		if( !isPrepared() ) return true;
		return mCacheWordService.isLocked();
	}

	private void checkCacheWordState() {
		// this is ugly as all hell

		int newState = Constants.STATE_UNKNOWN;

		if( !isCacheWordConnected() ) {
			newState = Constants.STATE_UNKNOWN;
			Log.d(TAG, "checkCacheWordState: not connected");
		} else if( !isCacheWordInitialized() ) {
			newState = Constants.STATE_UNINITIALIZED;
			Log.d(TAG, "checkCacheWordState: STATE_UNINITIALIZED");
		} else if( isCacheWordConnected() && mCacheWordService.isLocked() ) {
			newState = Constants.STATE_LOCKED;
			Log.d(TAG, "checkCacheWordState: STATE_LOCKED");
		} else {
			newState = Constants.STATE_UNLOCKED;
			Log.d(TAG, "checkCacheWordState: STATE_UNLOCKED");
		}

		if( newState == Constants.STATE_UNINITIALIZED ) {
			mSubscriber.onCacheWordUninitializedEvent();
		} else if( newState == Constants.STATE_LOCKED ) {
			mSubscriber.onCacheWordLockedEvent();
		} else if( newState == Constants.STATE_UNLOCKED ) {
			mSubscriber.onCacheWordUnLockedEvent();
		} else {
			Log.e(TAG, "Unknown CacheWord state entered!");
		}
	}

	private boolean isCacheWordConnected() {
		return mCacheWordService != null;
	}

	private boolean isCacheWordInitialized() {
		return SecretsManager.isInitialized(mContext);
	}

	private boolean isPrepared() {
		return isCacheWordConnected() && isCacheWordInitialized();
	}


	private BroadcastReceiver mCacheWordReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) {
			  if(intent.getAction().equals(Constants.INTENT_NEW_SECRETS)) {
				  Log.d(TAG, "New secrets received");
				  checkCacheWordState();
			  }
		  }
	};

	private ServiceConnection mCacheWordServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			ICacheWord cwBinder = (ICacheWord) binder;
			if( cwBinder != null ) {
				Log.d(TAG, "Connected to CacheWordService");
				mCacheWordService = cwBinder.getService();
				checkCacheWordState();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mCacheWordServiceConnection = null;
			mCacheWordService = null;
			checkCacheWordState();
		}

	};

}
