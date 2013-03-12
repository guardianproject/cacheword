package info.guardianproject.cacheword;

public class PassphraseSecrets implements ICachedSecrets {

	private final String mPassphrase;

	public PassphraseSecrets(String passphrase) {
		mPassphrase = passphrase;
	}

	public String getPassphrase() {
		return mPassphrase;
	}

}
