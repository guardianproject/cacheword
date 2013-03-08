package sample.cacheword;

import sample.cacheword.R.id;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.CachedSecrets;
import info.guardianproject.cacheword.CacheWordSubscriber;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CacheWordSampleActivity extends Activity implements
		CacheWordSubscriber {

	private static final String TAG = "CacheWordSampleActivity";

	private CacheWordHandler mCacheWord;

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
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				Log.d(TAG, "edit event");
				saveMessage(mSecretEdit.getText().toString());
			}
		});

		mSecretEdit.setEnabled(false);

		mCacheWord = new CacheWordHandler(this);
	}

	@Override
	protected void onResume() {
		super.onStart();
		mCacheWord.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCacheWord.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_cache_word_sample, menu);
		return true;
	}

	private void saveMessage(String contents) {
		if (mCacheWord.isLocked())
			return;

		String passphrase = mCacheWord.getCachedSecrets().getPassphrase();
		SecretMessage.saveMessage(this, passphrase, contents);
	}

	private void buttonClicked() {

		if (mCacheWord.isLocked()) {

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Enter your passphrase");
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);
			builder.setView(input);

			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String passphrase = input.getText().toString();

							Log.d(TAG, "User entered pass:" + passphrase);
							mCacheWord.setCachedSecrets(new CachedSecrets(
									passphrase));
						}
					});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});

			builder.show();

		} else {
			mCacheWord.manuallyLock();
		}
	}

	@Override
	public void onCacheWordLockedEvent() {
		Log.d(TAG, "onCacheWordLockedEvent()");
		mSecretEdit.clearComposingText();
		mSecretEdit.setText("");
		mSecretEdit.setEnabled(false);
		mLockButton.setText("Unlock Secrets");
		mStatusLabel.setText("Locked");

	}

	@Override
	public void onCacheWordUnLockedEvent() {
		Log.d(TAG, "onCacheWordUnLockedEvent()");
		mLockButton.setText("Lock Secrets");
		mSecretEdit.setEnabled(true);
		mStatusLabel.setText("Unlocked");

		// fetch the password from CacheWordService
		String passphrase = mCacheWord.getCachedSecrets().getPassphrase();
		String message = SecretMessage.retrieveMessage(this, passphrase);

		if (message == null) {
			mSecretEdit.setText("");
		} else {
			mSecretEdit.setText(message);
		}

	}

	@Override
	public void onCacheWordUninitializedEvent() {
		mStatusLabel.setText("Uninitialized");
		mSecretEdit.setEnabled(false);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Create a Passphrase");
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);
		builder.setView(input);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String passphrase = input.getText().toString();

				Log.d(TAG, "User entered pass:" + passphrase);
				mCacheWord.setCachedSecrets(new CachedSecrets(passphrase));
			}
		});
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});

		builder.show();

	}

}
