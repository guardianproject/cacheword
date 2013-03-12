package info.guardianproject.cacheword;

/**
 * A simple interface for notifying about state changes.
 */
public interface ICacheWordSubscriber {

	/**
	 * Called when the cached secrets are wiped from memory.
	 */
	public void onCacheWordLockedEvent();

	/**
	 * Called when the secrets become available.
	 */
	public void onCacheWordUnLockedEvent();

	/**
	 * Called when CacheWord is reset and there are no secrets to unlock.
	 */
	public void onCacheWordUninitializedEvent();

}
