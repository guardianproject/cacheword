
package sample.cacheword;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

import info.guardianproject.cacheword.CacheWordActivityHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;
import sample.cacheword.R.id;

/**
 * A sample activity demonstrating how to use CacheWord. <br>
 * There are three primary items of note:
 * <ol>
 * <li>1. The Activity implements the CacheWordSubscriber interface and handles
 * the state change methods.</li>
 * <li>2. a CacheWordActivityHandler is instantiated in onCreate()</li>
 * <li>3. in onResume and onPause the corresponding methods are called in the
 * CacheWordActivityHandler</li>
 * </ol>
 * These three items are required to successfully use CacheWord.
 */
public class CacheWordSampleActivity extends Activity implements
        ICacheWordSubscriber {

    private static final String TAG = "CacheWordSampleActivity";

    // our handler does all the work in talking to the CacheWordService
    private CacheWordActivityHandler mCacheWord;

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
        mSecretEdit.setEnabled(false);

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

        mCacheWord = new CacheWordActivityHandler(this);
    }

    @Override
    protected void onResume() {
        super.onStart();
        // Notify the CacheWordHandler
        mCacheWord.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Notify the CacheWordHandler
        mCacheWord.onPause();
    }

    private void saveMessage(String contents) {

        // ensure we're unlocked
        if (mCacheWord.isLocked())
            return;

        // fetch the encryption key from CacheWord
        SecretKey key = ((PassphraseSecrets) mCacheWord.getCachedSecrets()).getSecretKey();
        SecretMessage.saveMessage(this, key, contents);
    }

    private void buttonClicked() {

        // this button locks and unlocks the secret message
        if (mCacheWord.isLocked()) {
            // lets unlock!
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

                            // verify the passphrase with CacheWord

                            char[] passwd = passphrase.toCharArray();
                            PassphraseSecrets secrets;
                            try {
                                secrets = PassphraseSecrets.fetchSecrets(CacheWordSampleActivity.this, passwd);
                                mCacheWord.setCachedSecrets(secrets);
                            } catch (GeneralSecurityException e) {
                                // Invalid password or the secret key has been tampered with
                                // TODO(abel) handle bad password in sample app
                                Log.e(TAG, "invalid password or secrets has been tampered with");
                                Log.e(TAG, e.getClass().getName() + " : " + e.getMessage());
                                e.printStackTrace();
                            }

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
            // lock the secret message
            mCacheWord.manuallyLock();
        }
    }

    @Override
    public void onCacheWordLocked() {
        Log.d(TAG, "onCacheWordLocked()");

        // close up everything

        mSecretEdit.clearComposingText();
        mSecretEdit.setText("");
        mSecretEdit.setEnabled(false);
        mLockButton.setText("Unlock Secrets");
        mStatusLabel.setText("Locked");

    }

    @Override
    public void onCacheWordOpened() {
        Log.d(TAG, "onCacheWordOpened()");
        mLockButton.setText("Lock Secrets");
        mSecretEdit.setEnabled(true);
        mStatusLabel.setText("Unlocked");

        // fetch the encryption key from CacheWordService
        SecretKey key = ((PassphraseSecrets) mCacheWord.getCachedSecrets()).getSecretKey();
        String message = SecretMessage.retrieveMessage(this, key);

        if (message == null) {
            mSecretEdit.setText("");
        } else {
            mSecretEdit.setText(message);
        }

    }

    @Override
    public void onCacheWordUninitialized() {
        // if we're uninitialized, we want to initialize CacheWord with a new
        // passphrase
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

                mCacheWord.setCachedSecrets(PassphraseSecrets.initializeSecrets(CacheWordSampleActivity.this, passphrase.toCharArray()));
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
