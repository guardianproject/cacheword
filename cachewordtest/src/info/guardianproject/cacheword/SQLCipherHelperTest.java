
package info.guardianproject.cacheword;

import android.content.Context;
import android.test.ServiceTestCase;
import android.util.Log;

import net.sqlcipher.Cursor;
import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;

import java.security.GeneralSecurityException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SQLCipherHelperTest extends ServiceTestCase<CacheWordService> {

    public SQLCipherHelperTest() {
        super(CacheWordService.class);
    }

    public SQLCipherHelperTest(Class<CacheWordService> serviceClass) {
        super(serviceClass);
    }

    private final static String TAG = "SQLCipherHelperTest";

    private final static String DB_PASS = "hunter2";
    private final static String DB_NAME = "test.db";
    private final static int DB_VERSION = 1;
    CacheWordHandler mHandler = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SecretsManager.setInitialized(getContext(), false);
        SQLiteDatabase.loadLibs(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        getContext().getDatabasePath(DB_NAME).delete();
    }

    public void testWriteDatabase() {
        Log.d(TAG, "testWriteDatabase");
        final CountDownLatch signal = new CountDownLatch(1);
        TestWriteableDatabaseSubscriber subscriber = new TestWriteableDatabaseSubscriber(signal);
        mHandler = new CacheWordHandler(getContext(), subscriber);

        mHandler.connectToService();
        Log.d(TAG, "testWriteDatabase called connectToService");
        try {
            signal.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("testWriteDatabase timedout");
        }

    }

    class TestWriteableDatabaseSubscriber implements ICacheWordSubscriber {
        CountDownLatch signal;

        TestWriteableDatabaseSubscriber(CountDownLatch signal) {
            this.signal = signal;
        }

        boolean dataWritten = false;

        public TestWriteableDatabaseSubscriber() {
        }

        @Override
        public void onCacheWordUninitialized() {
            Log.d(TAG, "onCacheWordUninitialized");

            char[] pass = DB_PASS.toCharArray();

            try {
                mHandler.setPassphrase(pass);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                fail("Failed to initialize");
            }
        }

        @Override
        public void onCacheWordLocked() {
            Log.d(TAG, "onCacheWordLocked");

            try {
                DatabaseHelper db_helper = new DatabaseHelper(mHandler, getContext());
                SQLiteDatabase db = db_helper.getWritableDatabase();
                fail("onCacheWordLocked: Shouldn't be able to open locked database");
                db.close(); // silence unused warning
            } catch (SQLException e) {
                // pass
            }

            if (dataWritten) {
                char[] pass = DB_PASS.toCharArray();
                try {
                    mHandler.setPassphrase(pass);
                    Log.d(TAG, "onCacheWordLocked opening again...");
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    fail("Failed to unlock");
                }
            }
        }

        @Override
        public void onCacheWordOpened() {
            Log.d(TAG, "onCacheWordOpened");

            DatabaseHelper db_helper = new DatabaseHelper(mHandler, getContext());

            if (!dataWritten) {
                Log.d(TAG, "onCacheWordOpened: writing database");
                SQLiteDatabase db = db_helper.getWritableDatabase();

                try {
                    db.execSQL("CREATE TABLE foobar (one text, two int);");
                    db.execSQL("INSERT INTO foobar VALUES('Hello, World!', 99);");
                    dataWritten = true;
                    db.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    fail("SQLite exception when creating db structure");
                }
                mHandler.lock();

            } else {
                Log.d(TAG, "onCacheWordOpened: reading data");
                SQLiteDatabase db = db_helper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT * FROM foobar", null);
                assertNotNull(cursor);
                assertTrue(cursor.moveToFirst());
                String str = cursor.getString(0);
                int val = cursor.getInt(1);
                cursor.close();
                db.close();

                assertEquals("Hello, World!", str);
                assertEquals(99, val);
                signal.countDown();
            }

        }
    }

    public static class DatabaseHelper extends SQLCipherOpenHelper {

        public DatabaseHelper(CacheWordHandler cacheWord, Context context) {
            super(cacheWord, context, DB_NAME, null, DB_VERSION);
            Log.d(TAG, "DatabaseHelper ctor");
        }

        @Override
        public void onCreate(SQLiteDatabase arg0) {
            Log.d(TAG, "DatabaseHelper onCreate");

        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
            Log.d(TAG, "DatabaseHelper onUpgrade");

        }

    }
}
