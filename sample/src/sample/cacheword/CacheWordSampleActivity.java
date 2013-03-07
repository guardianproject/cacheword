package sample.cacheword;

import info.guardianproject.cacheword.CacheWordService;
import info.guardianproject.cacheword.CachedSecrets;
import info.guardianproject.cacheword.Constants;
import info.guardianproject.cacheword.ICacheWord;
import info.guardianproject.cacheword.SecretsManager;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

public class CacheWordSampleActivity extends Activity {

	private static final String TAG = "CacheWordSampleActivity";

	private CacheWordService mCacheWordService;
	private int mLastCacheWordState;

	TextView mStatusLabel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cache_word_sample);

		mStatusLabel = (TextView) findViewById(R.id.statusLabel);

		LocalBroadcastManager.getInstance(this).registerReceiver(mCacheWordReceiver, new IntentFilter(Constants.INTENT_NEW_SECRETS));
	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent cacheWordIntent = new Intent();
		cacheWordIntent.setClassName(getApplicationContext(), "info.guardianproject.cacheword.CacheWordService");
		bindService(cacheWordIntent, mCacheWordServiceConnection, Context.BIND_AUTO_CREATE);

		checkCacheWordState();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(mCacheWordService != null) {
			unbindService(mCacheWordServiceConnection);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_cache_word_sample, menu);
		return true;
	}

	private void checkCacheWordState() {
		// this is ugly as all hell, where's a FSM implementation when you need one

		int newState = Constants.STATE_UNKNOWN;

		if( !isCacheWordConnected() ) {
			newState = Constants.STATE_UNKNOWN;
			Log.d(TAG, "checkCacheWordState: not connected");
		} else if( !SecretsManager.isInitialized(this) ) {
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
			handleStateUninitialized();
		} else if( newState == Constants.STATE_LOCKED ) {
			handleStateLocked();
		} else if( newState == Constants.STATE_UNLOCKED ) {
			handleStateUnLocked();
		} else {
			handleUnknown();
		}

		mLastCacheWordState = newState;

	}

	private void handleStateUninitialized() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Create a Passphrase");
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		builder.setView(input);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		    	String passphrase = input.getText().toString();

		    	Log.d(TAG, "User entered pass:" + passphrase);
		    	mCacheWordService.setCachedSecrets(new CachedSecrets(passphrase));
		    }
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        dialog.cancel();
		    }
		});

		builder.show();

	}

	private void handleStateLocked() {
		Log.d(TAG, "handleStateLocked()");

	}

	private void handleStateUnLocked() {
		Log.d(TAG, "handleStateUnLocked()");

	}

	private void handleUnknown() {
		mStatusLabel.setText("Error");
	}

	private boolean isCacheWordConnected() {
		return mCacheWordService != null;
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
