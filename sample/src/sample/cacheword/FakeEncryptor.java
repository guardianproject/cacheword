package sample.cacheword;

import android.util.Base64;

/**
 * This class is merely for demonstration purposes.
 * It does not do real encryption.
 * DO NOT USE IT
 */
public class FakeEncryptor {

	public static String encrypt(String password, String message) {
		if(password == null || message == null ) return null;

		String plaintext = password + message;
		return Base64.encodeToString(plaintext.getBytes(), Base64.DEFAULT);

	}

	public static String decrypt(String password, String ciphertext) {
		if(password == null || ciphertext == null ) return null;

		byte[] raw = Base64.decode(ciphertext, Base64.DEFAULT);

		String plaintext = new String(raw);

		if(plaintext.startsWith(password))
			return plaintext.substring(password.length());
		return null;
	}

}
