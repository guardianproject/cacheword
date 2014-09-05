
package info.guardianproject.cacheword;

import android.util.Log;

import info.guardianproject.iocipher.VirtualFileSystem;

import java.io.IOException;

/**
 * A helper class that mounts an IOCipher virtual file system using the
 * encryption key managed by CacheWord.
 */
public class IOCipherMountHelper {
    private static String TAG = "IOCipherMountHelper";
    private CacheWordHandler mHandler;
    private static VirtualFileSystem mVFS;

    public IOCipherMountHelper(CacheWordHandler cacheWord) {
        if (cacheWord == null)
            throw new IllegalArgumentException("CacheWordHandler is null");
        mHandler = cacheWord;
    }

    /**
     * Mounts a VFS at path with encryption key from CacheWord, or returns the
     * previously mounted VFS if the path's are equal. Only one VFS per process
     * is supported, so opening a new VFS at a new path will close the existing
     * one.
     *
     * @param containerPath
     * @return the VFS
     * @throws IOException when the database is locked or mounting failed
     */
    public VirtualFileSystem mount(String containerPath) throws IOException {
        if (mVFS.isMounted() && containerPath.equals(mVFS.getContainerPath()))
            return mVFS;
        if (mHandler.isLocked())
            throw new IOException("Database locked. Decryption key unavailable.");

        try {
            mVFS = VirtualFileSystem.get();
            mVFS.mount(containerPath, mHandler.getEncryptionKey());
        } catch (Exception e) {
            Log.e(TAG, "mounting IOCipher failed at " + containerPath);
            throw new IOException(e.getMessage());
        }
        return mVFS;
    }
}
