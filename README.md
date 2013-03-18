CacheWord
=========

CacheWord is an Android library project for passphrase caching and management.
It helps app developers securely generate, store, and access secrets derived from a user's passphrase.

**CacheWord is still under development. DO NOT USE**

Broadly speaking this library assists developers with two related problems:

1. Secrets Management: how the secret key material for your app is generated, stored, and accessed
2. Passphrase Caching: store the passphrase in memory to avoid constantly prompting the user

CacheWord manages key derivation, verification, persistence, passphrase
resetting, and caching secret key material in memory.

*Features:*

* Strong key derivation (PBKDF2)
* Secure secret storage (AES-256 GCM)
* Persistent notification: informs the user the app data is unlocked
* Configurable timeout: after a specified time of inactivity the app locks itself
* Manual clearing: the user can forcibly lock the application
* Uses Android's Keystore on 4.x if available - *Not Yet Implemented*

#### Why?

Once the user has input her password into your application, what does your app
do with it? Do you just drop it in a static variable or in a singleton and let
the various bits of your app access it willy nilly?

TODO write more here

# Usage

## Setup

**(Eclipse) Import into your workspace**

Before we begin, download CacheWord and import it into your Eclipse workspace.
Then add it as a library project to your project in eclipse.

    Project Properties > Android > (Library) Add

TODO: non-eclipse instructions

**Edit your `AndroidManifest.xml`**

Add the following to between the `<application>....</application>` tags

```xml
<service android:name="info.guardianproject.cacheword.CacheWordService" android:enabled="true" android:exported="false" />
```

### Dependencies

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

The `ICacheWordSubscriber` interface consists of three state change methods.

**NOTE:** Strictly speaking these aren't state *change* methods, as they can be
invoked even when the state hasn't changed. For example, every call to
CacheWord's `onPause` or `onPause` resume will result in an event. This is so
new Activities can learn about CacheWord's state.

1. **onCacheWordUninitializedEvent**

    This event is called when CacheWord determines there is no saved state.
    Usually this occurs the first time the user runs your app. At this point there
    should be no sensitive data stored, because there is no secret to encrypt it
    with.

    In this event you should prompt the user to create a password and the pass the
    new password to CacheWord with `setCachedSecrets()`.

    This state could also be entered after the Application's data is cleared/reset.

2. **onCacheWordLockedEvent**

    This state signifies the secrets are unavailable or have become unavailable.
    It occurs when the secrets expiration timeout is reached, or the user
    manually locks CacheWord.

    You should clear all UI components and data structures containing sensitive
    information, perhaps show a dedicated lock screen.

    Prompt the user for the passphrase and pass it to CacheWord with
    `setCachedSecrets()`

3. **onCacheWordUnLockedEvent**

    This state is entered when CacheWord has received *valid* credentials via the
    `setCachedSecrets()` method.

    In this state you can call `getCachedSecrets()` to retrieve the unencrypted
    secrets from CacheWord.

**Example:**

```java
public class MyActivity extends Activity implements ICacheWordSubscriber
{

    ...

    @Override
    public void onCacheWordUninitializedEvent() {
        startCreatePassphraseActivity();
    }


    @Override
    public void onCacheWordLockedEvent() {
        clearUi();
        startPassphrasePromptActivity();
    }

    @Override
    public void onCacheWordUnLockedEvent() {
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

In most applications, much of your usual state handling code that usually goes
in `onResume` and `onPause` should instead go in one of the CacheWord event
methods.

### SQLCipher Support

If you use SQLCipher for encrypted database storage you should use CacheWord's
`SQLCipherOpenHelper` 

See the [cacheword branch][notecipher] in the NoteCipher application for an
example of how to use it.

# Library Development

See [HACKING.md](HACKING.md)

# Security Design Notes

See [SECURITY.md](SECURITY.md)

[notecipher]: https://github.com/guardianproject/notepadbot/tree/cacheword
[sqlcipher]: http://sqlcipher.net/sqlcipher-for-android/
