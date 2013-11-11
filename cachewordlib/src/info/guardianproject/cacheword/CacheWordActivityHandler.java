package info.guardianproject.cacheword;

import android.content.Context;

public class CacheWordActivityHandler extends CacheWordHandler {

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
