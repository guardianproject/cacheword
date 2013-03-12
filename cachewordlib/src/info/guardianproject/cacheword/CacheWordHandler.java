
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

/**
 * This class is designed to accompany any Activity that is interested in the
 * secrets cached by CacheWord. <i>The context provided in the constructor must
 * implement the ICacheWordSubscriber interface.</i> This is so the Activity can be
 * alerted to the state change events.
 */
public class CacheWordHandler {
    private static final String TAG = "CacheWordHandler";

    private Context mContext;
    private CacheWordService mCacheWordService;
    private ICacheWordSubscriber mSubscriber;

    /**
     * @param context must implement the ICacheWordSubscriber interface
     */
    public CacheWordHandler(Context context) {
        mContext = context;

        try {
            // shame we have to do this at runtime.
            // must ponder a way to enforce this relationship at compile time
            mSubscriber = (ICacheWordSubscriber) context;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                    "CacheWordHandler passed invalid Activity. Expects class that implements ICacheWordSubscriber");
        }

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mCacheWordReceiver,
                new IntentFilter(Constants.INTENT_NEW_SECRETS));
    }

    /**
     * Call this method in your Activity's onResume()
     */
    public void onResume() {
        Intent cacheWordIntent = CacheWordService
                .getBlankServiceIntent(mContext.getApplicationContext());
        mContext.startService(cacheWordIntent);
        mContext.bindService(cacheWordIntent, mCacheWordServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    /**
     * Call this method in your Activity's onPause()
     */
    public void onPause() {
        if (mCacheWordService != null) {
            mCacheWordService.detachSubscriber();
            mContext.unbindService(mCacheWordServiceConnection);
        }
    }

    /**
     * Fetch the secrets from CacheWord
     *
     * @return the secrets or null on failure
     */
    public ICachedSecrets getCachedSecrets() {
        if (!isCacheWordConnected())
            return null;

        return mCacheWordService.getCachedSecrets();
    }

    /**
     * Write the secrets into CacheWord, initializing the cache if necessary.
     *
     * @param secrets
     */
    public void setCachedSecrets(ICachedSecrets secrets) {
        if (!isCacheWordConnected())
            return;

        mCacheWordService.setCachedSecrets(secrets);
    }

    /**
     * Clear the secrets from memory. This is only a request to CacheWord. The
     * cache should not be considered wiped and locked until the onLockEvent is
     * received.
     */
    public void manuallyLock() {
        if (!isPrepared())
            return;
        mCacheWordService.manuallyLock();
    }

    /**
     * @return true if the cache is locked or uninitialized, false otherwise
     */
    public boolean isLocked() {
        if (!isPrepared())
            return true;
        return mCacheWordService.isLocked();
    }

    // / private helpers
    // /////////////////////////////////////////

    private void checkCacheWordState() {
        // this is ugly as all hell

        int newState = Constants.STATE_UNKNOWN;

        if (!isCacheWordConnected()) {
            newState = Constants.STATE_UNKNOWN;
            Log.d(TAG, "checkCacheWordState: not connected");
        } else if (!isCacheWordInitialized()) {
            newState = Constants.STATE_UNINITIALIZED;
            Log.d(TAG, "checkCacheWordState: STATE_UNINITIALIZED");
        } else if (isCacheWordConnected() && mCacheWordService.isLocked()) {
            newState = Constants.STATE_LOCKED;
            Log.d(TAG, "checkCacheWordState: STATE_LOCKED");
        } else {
            newState = Constants.STATE_UNLOCKED;
            Log.d(TAG, "checkCacheWordState: STATE_UNLOCKED");
        }

        if (newState == Constants.STATE_UNINITIALIZED) {
            mSubscriber.onCacheWordUninitializedEvent();
        } else if (newState == Constants.STATE_LOCKED) {
            mSubscriber.onCacheWordLockedEvent();
        } else if (newState == Constants.STATE_UNLOCKED) {
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
            if (intent.getAction().equals(Constants.INTENT_NEW_SECRETS)) {
                Log.d(TAG, "New secrets received");
                checkCacheWordState();
            }
        }
    };

    private ServiceConnection mCacheWordServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ICacheWordBinder cwBinder = (ICacheWordBinder) binder;
            if (cwBinder != null) {
                Log.d(TAG, "Connected to CacheWordService");
                mCacheWordService = cwBinder.getService();
                mCacheWordService.attachSubscriber();
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
