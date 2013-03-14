package sample.cacheword;

import android.util.Base64;
import android.util.Log;

import java.nio.CharBuffer;

import javax.crypto.SecretKey;

import info.guardianproject.cacheword.Wiper;

/**
 * This class is merely for demonstration purposes.
 * It does not do real encryption.
 * DO NOT USE IT
 */
public class FakeEncryptor {

	public static String encrypt(SecretKey key, String message) {
		if(key == null || message == null ) return null;

		// prepare sensitive variables
		return Base64.encodeToString(message.getBytes(Wiper.Utf8CharSet), Base64.DEFAULT);
	}

	// Since this is a sample, I don't bother trying to keep the password
	// out of a String object.
	public static String decrypt(SecretKey key, String ciphertext) {
		if(key == null || ciphertext == null ) return null;

		byte[] raw_bytes = Base64.decode(ciphertext, Base64.DEFAULT);
		return new String(raw_bytes, Wiper.Utf8CharSet);
	}

}
