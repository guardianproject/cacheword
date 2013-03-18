# Building CacheWord

## Dependencies

**SQLCipher for Android**

Version 2.1.1 [Source][sqlcipher-github]

CacheWord depends on the class files for [SQLCipher for Android][sqlcipher], but
not the native shared libraries nor assets. To minimize disk space, only the
jars are included in this repo.

When upgrading SQLCipher for Android do:

    cp -r sqlcipher-for-android/libs/* CacheWord/cachwordlib/libs/
    rm -rf CacheWord/cachwordlib/libs/armeabi
    rm -rf CacheWord/cachwordlib/libs/x86


Applications using CacheWord must provide all the files.


[sqlcipher-github]: https://github.com/sqlcipher/android-database-sqlcipher
[sqlcipher]: http://sqlcipher.net/sqlcipher-for-android/
