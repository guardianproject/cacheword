package info.guardianproject.cacheword;

import android.content.Context;

public class CacheWordActivityHandler extends CacheWordHandler {
    /**
     *  @see {@link info.guardianproject.cacheword.CacheWordHandler#CacheWordHandler(Context context, CacheWordSettings settings)}
     */
    public CacheWordActivityHandler(Context context, CacheWordSettings settings) {
        super(context, settings);
    }

    /**
     *  @see {@link info.guardianproject.cacheword.CacheWordHandler#CacheWordHandler(Context context, ICacheWordSubscriber subscriber, CacheWordSettings settings)}
     */
    public CacheWordActivityHandler(Context context, ICacheWordSubscriber sub, CacheWordSettings settings) {
        super(context, sub, settings);
    }
    /**
     *  @see {@link info.guardianproject.cacheword.CacheWordHandler#CacheWordHandler(Context context)}
     */
    public CacheWordActivityHandler(Context context) {
        super(context);
    }

    /**
     *  @see {@link info.guardianproject.cacheword.CacheWordHandler#CacheWordHandler(Context context, ICacheWordSubscriber sub)}
     */
    public CacheWordActivityHandler(Context context, ICacheWordSubscriber sub) {
        super(context, sub);
    }

    /**
     * Call this method in your Activity's onResume()
     */
    public void onResume() {
        connectToService();
    }

    /**
     * Call this method in your Activity's onPause()
     */
    public void onPause() {
        disconnect();
    }

}
