CacheWord
=========

CacheWord is an Android library project for passphrase caching and management.
It helps app developers securely generate, store, and access secrets derived
from a user's passphrase.

**CacheWord is still under development. Proceed with caution**

Broadly speaking this library assists developers with two related problems:

1. Secrets Management: how the secret key material for your app is generated,
   stored, and accessed
2. Passphrase Caching: store the passphrase in memory to avoid constantly
   prompting the user

CacheWord manages key derivation, verification, persistence, passphrase
resetting, and caching secret key material in memory.

*Features:*

* Strong key derivation (PBKDF2)
* Secure secret storage (AES-256 GCM)
* Persistent notification: informs the user the app data is unlocked
* Configurable timeout: after a specified time of inactivity the app locks itself
* Manual clearing: the user can forcibly lock the application
* Uses Android's Keystore on 4.x if available - *Not Yet Implemented*

CacheWord requires at least SDK version 2.2 (API level 8)

# Usage

## Setup

**(Eclipse) Import into your workspace**

Before we begin, download CacheWord and import it into your Eclipse workspace.
Then add it as a library project to your project in eclipse.

    Project Properties > Android > (Library) Add

**(Ant) Add the library project to your project.properties**

You need to run `android update project -p .` inside the
`CacheWord/cachewordlib` folder, then add the path to your `project.properties`
file.  For more information please see the [Android developer guide][libguide] for
referencing library projects.

**Edit your `AndroidManifest.xml`**

Add the following to between the `<application>....</application>` tags

```xml
<service android:name="info.guardianproject.cacheword.CacheWordService" android:enabled="true" android:exported="false" />
```

### Dependencies

* Android support library v4 (`android-support-v4.jar`; included)
* [SQLCipher for Android >= v3.0.2][sqlcipher] (included)

CacheWord provides a support class for SQLCipher for Android. You probably want
to use this.

Download the [SQLCipher for Android v3.0.2 release][sqlcipher] and copy the `libs/`
and `assets/` dir into your Android project dir.

## Integration

A CacheWordSubscriber is any component in your application interested in the
secrets managed by CacheWord. Such components may be:

* Initialization Activity
* Login Activity
* Any Activity that handles sensitive data
* Encryption/Decryption wrappers
* SQLCipher Database Helper
* IOCipher Virtual File System
* etc.

For each of these interested components you *must* implement two things

1. Implement the `ICacheWordSubscriber` interface
2. Instantiate a `CacheWordHandler` to assist the component

**TIP**: Activities should implement `CacheWordActivityHandler` and propagate
the lifecycle methods `onPause` and `onResume` instead of calling [dis]connect().

### 1. Implementing `ICacheWordSubscriber`

The `ICacheWordSubscriber` interface consists of three event methods.

These event methods are similar to the Android lifecycle methods onResume,
onPause, etc. The appropriate event is guaranteed to be called for every
CacheWord enhanced Activity during the onResume method when then lifecycle
change is propagated correctly (see below). This ensures your Activities will
always be aware of the current status of CacheWord.

In most applications, your state and view handling code that usually goes
in `onResume` and `onPause` should instead go in one of the CacheWord event
methods.

1. **onCacheWordUninitialized**

    This event is called when CacheWord determines there is no saved state.
    Usually this occurs the first time the user runs your app. At this point there
    should be no sensitive data stored, because there is no secret to encrypt it
    with.

    In this event you should prompt the user to create a password and the pass the
    new password to CacheWord with `setCachedSecrets()`.

    This event could also be triggered after the Application's data is cleared or reset.

2. **onCacheWordLocked**

    This event signifies the secrets are unavailable or have become unavailable.
    It is triggered when the secrets expiration timeout is reached, or the user
    manually locks CacheWord.

    You should clear all UI components and data structures containing sensitive
    information and perhaps show a dedicated lock screen.

    At this stage your app should prompt the user for the passphrase and give
    it to CacheWord with `setCachedSecrets()`

3. **onCacheWordOpened**

    This event is triggered when CacheWord has received *valid* credentials via the
    `setCachedSecrets()` method.

    At this stage in your app you may call `getCachedSecrets()` to retrieve the
    unencrypted secrets from CacheWord.

**Example:**

```java
public class MyActivity extends Activity implements ICacheWordSubscriber
{

    ...

    @Override
    public void onCacheWordUninitialized() {
        startCreatePassphraseActivity();
    }


    @Override
    public void onCacheWordLocked() {
        clearUi();
        startPassphrasePromptActivity();
    }

    @Override
    public void onCacheWordOpened() {
        decryptDataAndPopulateUi(mCacheWord.getEncryptionKey());
    }

    ...
}

```

### 2. Instantiate CacheWordHandler and propagate lifecycle Changes

`CacheWordHandler` is the class instantiated by any object interested in
receiving CacheWord events. It does the heavy lifting of starting, connecting,
and communicating with the `CacheWordService`. Each object that wants to be aware
of CacheWord events should instantiate its own `CacheWordHandler`.

It is your object's responsibility to call
`CacheWordHandler.connectToService()` and `CacheWordHandler.disconnect()` when
your object wants to register and unregister from event notifications.

Disconnecting is important, because CacheWord maintains a list of connected
clients and will not initiate the automatic timeout until all the connected
clients have disconnected.

```java

class YourClass implements ICacheWordSubscriber
{
        ...
        private CacheWordHandler mCacheWord;
        ...
        public YourClass() {
            ...
            mCacheWord = new CacheWordHandler(mContext, this);
            mCacheWord.connectToService()
            ...
        }

        // Called when your object no longer wants to be notified of CacheWord events
        public goodbyeYourClass() {
            mCacheWord.disconnect()
        }
        ...
}
```


#### Activity's and CacheWordActivityHandler

Most of the time it is `Activity` classes that need to instantiate
`CacheWordHandler`, so for this use case there is a convenient class called
`CacheWordActivityHandler`. Instead of calling `connectToService` and
`disconnect()`, you simply need to propagate the Android lifecycle changes
`onPause()` and `onResume()`.

```java
class YourActivity extends Activity implements ICacheWordSubscriber
{
        ...
        private CacheWordActivityHandler mCacheWord;
        ...
        @Override
        public void onCreate(Bundle savedInstanceState) {
            ...
            mCacheWord = new CacheWordActivityHandler(this);
            ...
        }

        @Override
        protected void onResume() {
            super.onStart();
            mCacheWord.onResume();
        }

        @Override
        protected void onPause() {
            super.onPause();
            mCacheWord.onPause();
        }
        ...
}
```

## Common Usage Questions

### How do I configure CacheWord?

Configuration is entirely optional as sane defaults are provided for every
option.

For compile time configuration options, define the resources in XML (see
[`res/values/cacheword.xml`](cachewordlib/res/values/cacheword.xml) for the
available options).

To configure the XML resources, simply copy cacheword.xml into your own
`res/values` directory and edit the settings as you like.

Runtime configuration options can be set via the CacheWordSettings object.

Configurable options are:

* Whether to show a persistent unlocked notification
* Display options for the notification (title, message, icon, etc)
* Intent executed when the notification is clicked
* Timeout length after your app leaves the foreground
* Vibration on successful unlock
* PBKDF2 Calibration settings and minimum iteration count

### How do I use CW with SQLCipher & IOCipher?

If you use SQLCipher for encrypted database storage you should use CacheWord's
`SQLCipherOpenHelper`. See the [NoteCipher application][notecipher] for an
example of how to use it.

Likewise if you use IOCipher for encrypted file storage you should use CacheWord's `IOCipherHelper`.

TODO: make example of IOCipherHelper


### What Are These Cached Secrets?

The sensitive data that is cached by CacheWord can be specified by the user as
a class implementing `ICachedSecrets`.

The default implementation of this class (`PassphraseSecrets`) attempts to
provide for most use cases. It generates a 256 bit encryption key that can be
used by other libraries like [SQLCipher][sqlcipher] or [IOCipher][iocipher]

In this case the user's password is used to encrypt (after being hashed of
course) the generated encryption key, and is never written to disk.

### How does CacheWord work with background services?

Many apps will perform operations in the background that require access to
sensitive data, even though the user may not be actively using the app. These
services will require access to the cached secrets in order to write to the
database, update internal structure, etc.

For example, a chat application will run a service in the background to check
for new messages on the wire, and then write them to the database.

Since the background service needs access to the cached secrets, the app must
remain unlocked. If the configured timeout occurs, the CacheWord will lock, and
the background service will be unable to do its job.

If the user closes the app, they may well expect it to be locked, after all
they aren't using it and they want to protect their data, but, in the case of
the messenger app, they will still want to be notified when a new message arrives.

The results in inconsistent expectations, you can't lock the app without
shutting down the background service, but you need the background service
running to provide some basic functionality.

How this is handled in your app depends on what your background service is
doing.

If the user expects your app to do something in the background and notify them,
then you will need to disable the auto-timeout in CacheWord. Likewise, if the
user locks the app, they should be aware that they will be disabling any
background functionality.

You might be tempted to cache the secrets yourself, in an global variable or
singleton, so that your app can appear to be locked (the user has to enter a
passphrase), but the background service can still work. *This is a bad idea!*
By doing this you lose the secure memory handling CacheWord employs, and negate
much of the benefit derived from using CW.

# Issues & Support

Bug? Please report any issues at [our project issue tracker][issues], no
account required!

Question? Find us on IRC or our mailing list.

* IRC: #guardianproject @ freenode
* Mailing List: [guardian-dev](https://lists.mayfirst.org/mailman/listinfo/guardian-dev)

# Library Development

See [HACKING.md](HACKING.md)

# Security Design Notes

See [SECURITY.md](SECURITY.md)

[notecipher]: https://github.com/guardianproject/notepadbot/
[sqlcipher]: http://sqlcipher.net/sqlcipher-for-android/
[iocipher]: https://guardianproject.info/code/IOCipher
[issues]: https://dev.guardianproject.info/projects/cacheword/issues/new
[libguide]: http://developer.android.com/guide/developing/projects/projects-cmdline.html#ReferencingLibraryProject
