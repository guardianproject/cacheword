package sample.cacheword;

import android.util.Base64;
import android.util.Log;

import java.nio.CharBuffer;
import info.guardianproject.cacheword.Wiper;

/**
 * This class is merely for demonstration purposes.
 * It does not do real encryption.
 * DO NOT USE IT
 */
public class FakeEncryptor {

	public static String encrypt(char[] password, String message) {
		if(password == null || message == null ) return null;

		// prepare sensitive variables
		byte [] plaintext_bytes = null;
		CharBuffer cb = null;
		char[] msg_chars = message.toCharArray();

		try {

		    // append pass and message
		    cb = CharBuffer.allocate(password.length + msg_chars.length);
		    cb.put(password);
		    cb.put(message);

		    // convert into UTF-8 encoded bytes
		    plaintext_bytes = Wiper.utf8charsToBytes(cb.array());

		    // stringify for storage
    		return Base64.encodeToString(plaintext_bytes, Base64.DEFAULT);

		} finally {
		    // wipe sensitive variables
		    Wiper.wipe(cb);
		    Wiper.wipe(plaintext_bytes);
		    Wiper.wipe(msg_chars);
		}

	}

	// Since this is a sample, I don't bother trying to keep the password
	// out of a String object.
	public static String decrypt(char[] password, String ciphertext) {
		if(password == null || ciphertext == null ) return null;

		byte[] raw_bytes = Base64.decode(ciphertext, Base64.DEFAULT);
		char[] chars = Wiper.bytesToUtf8Chars(raw_bytes);

		final String plaintext = new String(chars);
		Log.d("FE",  "plaintext: " + plaintext);
		// Instead of converting the password to a String
		// I should write a loop that performs a compare like startsWit
		// but this is an example application, so I won't
		final String INSECURE_PASSWORD_DO_NOT_DO_THIS = new String(password);
		Log.d("FE",  "INSECURE_PASSWORD_DO_NOT_DO_THIS: " + INSECURE_PASSWORD_DO_NOT_DO_THIS);

		Log.d("FakeEncryptor", "decrypt pass="+INSECURE_PASSWORD_DO_NOT_DO_THIS + " plaintext="+plaintext + " len="+raw_bytes.length);
		if(plaintext.startsWith(INSECURE_PASSWORD_DO_NOT_DO_THIS)) {
			String msg = plaintext.substring(password.length);
			Log.d("FakeEncryptor", "INSIDE decrypt pass="+password.toString() + " plaintext="+plaintext + " msg=" + msg + " msg len="+msg.length()+ " len="+raw_bytes.length);
			return msg;
		}
		return null;
	}

}
