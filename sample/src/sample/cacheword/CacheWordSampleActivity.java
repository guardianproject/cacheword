package sample.cacheword;

import sample.cacheword.R.id;
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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class CacheWordSampleActivity extends Activity {

	private static final String TAG = "CacheWordSampleActivity";

	private CacheWordService mCacheWordService;
	private int mLastCacheWordState;

	private TextView mStatusLabel;
	private Button mLockButton;
	private EditText mSecretEdit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cache_word_sample);

		mStatusLabel = (TextView) findViewById(R.id.statusLabel);
		mLockButton = (Button) findViewById(id.lockButton);
		mSecretEdit = (EditText) findViewById(R.id.secretEdit);

		mLockButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				buttonClicked();
			}
		});

		mSecretEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				Log.d(TAG, "edit event");
				saveMessage(mSecretEdit.getText().toString());
			}
		});

		mSecretEdit.setEnabled(false);

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
		mStatusLabel.setText("Uninitialized");
		mSecretEdit.setEnabled(false);

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
		mSecretEdit.clearComposingText();
		mSecretEdit.setText("");
		mSecretEdit.setEnabled(false);
		mLockButton.setText("Unlock Secrets");
		mStatusLabel.setText("Locked");

	}

	private void handleStateUnLocked() {
		Log.d(TAG, "handleStateUnLocked()");
		mLockButton.setText("Lock Secrets");
		mSecretEdit.setEnabled(true);
		mStatusLabel.setText("Unlocked");

		//fetch the password from CacheWordService
		String passphrase = mCacheWordService.getCachedSecrets().getPassphrase();
		String message = SecretMessage.retrieveMessage(this, passphrase);

		if( message == null ) {
			mSecretEdit.setText("");
		} else {
			mSecretEdit.setText(message);
		}
	}

	private void handleUnknown() {
		mStatusLabel.setText("Error");
	}

	private void saveMessage(String contents) {
		if( !isCacheWordConnected() || mCacheWordService.isLocked() ) return;

		String passphrase = mCacheWordService.getCachedSecrets().getPassphrase();
		SecretMessage.saveMessage(this, passphrase, contents);
	}

	private void buttonClicked() {
		if( !isCacheWordConnected() ) return;

		if( mCacheWordService.isLocked() ) {

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Enter your passphrase");
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

		} else {
			mCacheWordService.manuallyLock();
		}
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
