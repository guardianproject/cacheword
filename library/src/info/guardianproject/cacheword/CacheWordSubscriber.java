package info.guardianproject.cacheword;

public interface CacheWordSubscriber{

	public void onCacheWordLockedEvent();
	public void onCacheWordUnLockedEvent();
	public void onCacheWordUninitializedEvent();

}
