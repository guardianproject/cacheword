package info.guardianproject.cacheword;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase.CursorFactory;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteOpenHelper;

import org.apache.commons.codec.binary.Hex;


/**
 * A helper class to manage database creation and version management.
 * You create a subclass implementing {@link #onCreate}, {@link #onUpgrade} and
 * optionally {@link #onOpen}, and this class takes care of opening the database
 * if it exists, creating it if it does not, and upgrading it as necessary.
 * Transactions are used to make sure the database is always in a sensible state.
 * <p>For an example, see the NotePadProvider class in the NotePad sample application,
 * in the <em>samples/</em> directory of the SDK.</p>
 */
public abstract class SQLCipherOpenHelper extends SQLiteOpenHelper {

    protected Context mContext; // shame we have to duplicate this here
    private CacheWordHandler mHandler;

    public SQLCipherOpenHelper(CacheWordHandler cacheWord, Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
        if( cacheWord == null ) throw new IllegalArgumentException("CacheWordHandler is null");
        mHandler = cacheWord;
    }

    /**
     * Create and/or open a database that will be used for reading and writing.
     * Once opened successfully, the database is cached, so you can call this
     * method every time you need to write to the database.  Make sure to call
     * {@link #close} when you no longer need it.
     *
     * <p>Errors such as bad permissions or a full disk may cause this operation
     * to fail, but future attempts may succeed if the problem is fixed.</p>
     *
     * @throws SQLiteException if the database cannot be opened for writing
     * @return a read/write database object valid until {@link #close} is called
     */
    public synchronized SQLiteDatabase getWritableDatabase() {
        if( mHandler.isLocked() ) throw new SQLiteException("Database locked. Decryption key unavailable.");

        return super.getWritableDatabase(encodeRawKey(mHandler.getEncryptionKey()));
    }

    /**
     * Create and/or open a database.  This will be the same object returned by
     * {@link #getWritableDatabase} unless some problem, such as a full disk,
     * requires the database to be opened read-only.  In that case, a read-only
     * database object will be returned.  If the problem is fixed, a future call
     * to {@link #getWritableDatabase} may succeed, in which case the read-only
     * database object will be closed and the read/write object will be returned
     * in the future.
     *
     * @throws SQLiteException if the database cannot be opened
     * @return a database object valid until {@link #getWritableDatabase}
     *     or {@link #close} is called.
     */
    public synchronized SQLiteDatabase getReadableDatabase() {
        if( mHandler.isLocked() ) throw new SQLiteException("Database locked. Decryption key unavailable.");

        return super.getReadableDatabase(encodeRawKey(mHandler.getEncryptionKey()));
    }

    /**
     * Formats a byte sequence into the literal string format expected by
     * SQLCipher: hex'HEX ENCODED BYTES'
     * The key data must be 256 bits (32 bytes) wide.
     * The key data will be formatted into a 64 character hex string, and the returned
     * string will be exactly 67 characters in length.
     * @link http://sqlcipher.net/sqlcipher-api/#key
     * @param raw_key a 32 byte array
     * @return the encoded key
     */
    private static String encodeRawKey(byte[] raw_key) {
        if( raw_key.length != 32 ) throw new IllegalArgumentException("provided key not 32 bytes (256 bits) wide");

        final String kPrefix = "x'";
        final String kSuffix = "'";

        final char[] key_chars = Hex.encodeHex(raw_key);
        if( key_chars.length != 64 ) throw new IllegalStateException("encoded key is not 64 bytes wide");

        return kPrefix + new String(key_chars) + kSuffix;
    }

}
