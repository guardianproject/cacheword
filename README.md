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

CacheWord requires at least SDK version 2.3.3

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
* [SQLCipher for Android][sqlcipher] (included)

CacheWord provides a support class for SQLCipher for Android. You probably want
to use this.

Download the [SQLCipher for Android sources][sqlcipher] and copy the `libs/`
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

For each of these interested components you *must* implement three things

1. Implement the `ICacheWordSubscriber` interface
2. Instantiate a `CacheWordHandler` to assist the component
3. Call `connect()` and `disconnect()` as appropriate

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

### 2. Instantiate CacheWordHandler

```java

class YourClass implements ICacheWordSubscriber
{
        ...
        private CacheWordHandler mCacheWord;
        ...
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            ...
            mCacheWord = new CacheWordHandler(this);
            ...
        }
        ...
}
```

### 3. Propagate Lifecycle changes

```java
class YourClass implements ICacheWordSubscriber
{
        ...
        private CacheWordActivityHandler mCacheWord;
        ...
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

### Configuration

Configuration is entirely optional as sane defaults are provided for every
option.

For compile time configuration options, define the resources in XML (see
[`res/values/cacheword.xml`](cachewordlib/res/values/cacheword.xml) for the
available options).

Runtime configuration options can be set via the preferences.

Configurable options are:

* Whether to show a persistent unlocked notification
* Every aspect of the notification (title, message, icon, etc)
* Timeout length after your app leaves the foreground
* Vibration on successful unlock

### SQLCipher Support

If you use SQLCipher for encrypted database storage you should use CacheWord's
`SQLCipherOpenHelper` 

See the [cacheword branch][notecipher] in the NoteCipher application for an
example of how to use it.

### What Are These Cached Secrets?

The sensitive data that is cached by CacheWord can be specified by the user as
a class implementing `ICachedSecrets`.

The default implementation of this class (`PassphraseSecrets`) attempts to
provide for most use cases. It generates a 256 bit encryption key that can be
used by other libraries like [SQLCipher][sqlcipher] or [IOCipher][iocipher]

In this case the user's password is used to encrypt (after being hashed of
course) the generated encryption key, and is never written to disk.

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

[notecipher]: https://github.com/guardianproject/notepadbot/tree/cacheword
[sqlcipher]: http://sqlcipher.net/sqlcipher-for-android/
[iocipher]: https://guardianproject.info/code/IOCipher
[issues]: https://dev.guardianproject.info/projects/cacheword/issues/new
[libguide]: http://developer.android.com/guide/developing/projects/projects-cmdline.html#ReferencingLibraryProject
