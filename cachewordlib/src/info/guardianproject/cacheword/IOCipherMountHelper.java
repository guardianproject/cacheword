package info.guardianproject.cacheword;

import android.util.Log;

import info.guardianproject.iocipher.VirtualFileSystem;

import java.io.IOException;


/**
 * A helper class that mounts an IOCipher virtual file system using the encryption key managed by CacheWord.
 */
public class IOCipherMountHelper {
    private static String TAG = "IOCipherMountHelper";
    private CacheWordHandler mHandler;
    private String mMountPoint = new String();
    private static VirtualFileSystem mVFS;
    private boolean mMounted = false;

    public IOCipherMountHelper(CacheWordHandler cacheWord) {
        if( cacheWord == null ) throw new IllegalArgumentException("CacheWordHandler is null");
        mHandler = cacheWord;
    }

    /**
     * Mounts a VFS at path with encryption key from CacheWord, or returns
     * the previously mounted VFS if the path's are equal.
     *
     * Only one VFS per process is supported, so opening a new VFS
     * at a new path will close the existing one.
     *
     * @param path
     * @return the VFS
     * @throws IOException when the database is locked or mounting failed
     */
    public VirtualFileSystem mount(String path) throws IOException {
        if (mMounted && path.equals(mMountPoint)) return mVFS;
        if( mHandler.isLocked() ) throw new IOException("Database locked. Decryption key unavailable.");

        mMountPoint = path;
        try {
            mVFS = new VirtualFileSystem(mMountPoint);
            mVFS.mount(SQLCipherOpenHelper.encodeRawKeyToStr(mHandler.getEncryptionKey()));
        } catch (Exception e) {
            mMounted = false;
            Log.e(TAG, "mounting IOCipher failed at " + path );
            throw new IOException(e.getMessage());
        }
        mMounted = true;
        return mVFS ;
    }
}
