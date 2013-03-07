package info.guardianproject.cacheword;

public class CachedSecrets {

	private final String mPassphrase;

	public CachedSecrets(String passphrase) {
		mPassphrase = passphrase;
	}

	public String getPassphrase() {
		return mPassphrase;
	}

}
