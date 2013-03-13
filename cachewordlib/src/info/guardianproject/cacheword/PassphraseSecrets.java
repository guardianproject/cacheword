package info.guardianproject.cacheword;

public class PassphraseSecrets implements ICachedSecrets {

	private final char[] mPassphrase;

	public PassphraseSecrets(char[] passphrase) {
		mPassphrase = passphrase;
	}

	public char[] getPassphrase() {
		return mPassphrase;
	}

}
