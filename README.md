CacheWord
=========

CacheWord is an Android library project for passphrase caching and management.
It helps app developers securely generate, store, and access secrets derived
from a user's passphrase.

**CacheWord is still under development. Proceed with caution**

Broadly speaking this library assists developers with two related problems:

1. Secrets Management: how the secret key material for your app is generated,
   stored, and accessed
2. Passphrase Caching: store the passphrase in memory to avoid constantly
   prompting the user

CacheWord manages key derivation, verification, persistence, passphrase
resetting, and caching secret key material in memory.

**Features:**

* Strong key derivation (PBKDF2)
* Secure secret storage (AES-256 GCM)
* Persistent notification: informs the user the app data is unlocked
* Configurable timeout: after a specified time of inactivity the app locks itself
* Manual clearing: the user can forcibly lock the application
* Uses Android's Keystore on 4.x if available - *Not Yet Implemented*

CacheWord requires at least SDK version 2.3.1 (API level 9)

**Issues & Support**

* Bug? Please report any issues at [our project issue tracker][issues], no
account required!
* Questions? Find us on [IRC or our mailing list](https://guardianproject.info/contact)


# Table of Contents

- [CacheWord](#user-content-cacheword)
- [Table of Contents](#user-content-table-of-contents)
- [Setup](#user-content-setup)
	- [Dependencies](#user-content-dependencies)
- [Integration](#user-content-integration)
	- [Implementing ICacheWordSubscriber](#user-content-implementing-icachewordsubscriber)
	- [Instantiate CacheWordHandler and propagate lifecycle Changes](#user-content-instantiate-cachewordhandler-and-propagate-lifecycle-changes)
- [Common Usage Questions](#user-content-common-usage-questions)
	- [How do I configure CacheWord?](#user-content-how-do-i-configure-cacheword)
	- [How do I use CW with SQLCipher & IOCipher?](#user-content-how-do-i-use-cw-with-sqlcipher--iocipher)
	- [What Are These Cached Secrets?](#user-content-what-are-these-cached-secrets)
	- [How does CacheWord work with background services?](#user-content-how-does-cacheword-work-with-background-services)
- [Security Design Notes](#user-content-security-design-notes)
	- [Threat Model](#user-content-threat-model)
	- [Key Derivation and Encryption Key Generation](#user-content-key-derivation-and-encryption-key-generation)
	- [Managing Key Material Securely in Memory](#user-content-managing-key-material-securely-in-memory)
	- [Official Authorities On The Use of String](#user-content-official-authorities-on-the-use-of-string)


# Setup

**(Eclipse) Import into your workspace**

Before we begin, download CacheWord and import it into your Eclipse workspace.
Then add it as a library project to your project in eclipse.

    Project Properties > Android > (Library) Add

**(Ant) Add the library project to your project.properties**

To add this to you project, include something like this in your
`project.properties`:

```
android.library.reference.1=../CacheWord/cachewordlib
```

To build with ant, you need to run `./setup-ant` inside the `CacheWord/`
folder.  For more information please see the
[Android developer guide][libguide] for referencing library projects.

**Edit your `AndroidManifest.xml`**

You can use the the "manifest merging" feature of recent Android ADT releases
to get the required meta data into your project.  That is done by adding this
to your project's `project.properties`:

```
manifestmerger.enabled=true
```

Otherwise, you can manually add the metadata by copying and pasting the
relevant bits from `cachewordlib/AndroidManifest.xml`.


## Dependencies

* Android support library v4 (`android-support-v4.jar`; included)
* [SQLCipher for Android >= v3.0.2][sqlcipher] (included)

CacheWord provides a support class for SQLCipher for Android. You probably want
to use this.

Download the [SQLCipher for Android v3.0.2 release][sqlcipher] and copy the `libs/`
and `assets/` dir into your Android project dir.


# Integration

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

## Implementing `ICacheWordSubscriber`

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

## Instantiate CacheWordHandler and propagate lifecycle Changes

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


# Common Usage Questions

## How do I configure CacheWord?

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

## How do I use CW with SQLCipher & IOCipher?

If you use SQLCipher for encrypted database storage you should use CacheWord's
`SQLCipherOpenHelper`. See the [NoteCipher application][notecipher] for an
example of how to use it.

Likewise if you use IOCipher for encrypted file storage you should use CacheWord's `IOCipherHelper`.

TODO: make example of IOCipherHelper

## What Are These Cached Secrets?

The sensitive data that is cached by CacheWord can be specified by the user as
a class implementing `ICachedSecrets`.

The default implementation of this class (`PassphraseSecrets`) attempts to
provide for most use cases. It generates a 256 bit encryption key that can be
used by other libraries like [SQLCipher][sqlcipher] or [IOCipher][iocipher]

In this case the user's password is used to encrypt (after being hashed of
course) the generated encryption key, and is never written to disk.

## How does CacheWord work with background services?

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

The `CacheWordHandler.detach()` and `reattach()` methods are available, which
will allow your background service to receive CW events but not be counted
among the connected clients when the automatic lock timeout occurs. In other
words, the background service won't prevent the lock timeout from happening if
it is still running. Your service should properly handle Lock events, even if
it is in the middle of a running operation.



# Security Design Notes

The goal of CacheWord is to provide easy and secure "secrets" protection for
Android developers. By "secrets" we mean sensitive key material such as
passwords and encryption keys. We specifically exclude sensitive application
data (such as the data encrypted by said keys) from secrets we aim to protect.

## Threat Model

CacheWord assumes three distinct adversaries.

1. **The Developer**

  If the developer has malicious intentions, everything we do is bust. Instead,
  we aim to mitigate weaknesses posed by a careless or ignorant developer.
  
  Tactics:
  * Hide all crypto decisions
  * Use secure defaults
  * Provide support classes for commonly used libraries that consume secret key material (e.g., SQLCipher)

2. **The User**

  Once again the if user is intentionally trying to disclose secret key material,
  it is unlikely we can stop her. We do not think this is a common case however.
  Most users will unintentionally harm their security due to usability issues.
  
  For example, typing a password with *sufficient* entropy on a smartphone's soft
  keyboard is a severe pain in the ass for all but the most proficient of tween
  txtrs. Even this speedy group will grow weary when their phone prompts them for
  the password every time they pull it out of their pocket. Unsurprisingly, users
  choose short, low entropy passwords.
  
  Users often reuse passwords, so the protection of their password should be
  considered more important than an application specific encryption key.
  
  Tactics:
  * Sane cache timeouts
  * Password hashing using a strong KDF (PBKDF2, and hopefully scrypt soon)
  * Adaptive KDF iterations

3. **The Bad Guys**

  The Bad Guys consist of a number of potential adversaries, such as forensic analysts,
  cops or border agents with [plug-n-pwn data suckers][cellibrite], and malware.
  
  Their common capability in our case is access to our application's binary,
  memory and disk. They probably have root access too. Strictly speaking, given
  an attacker with sufficient patient and skill, our secrets will become theirs.
  
  That said, we strive to make key recovery from memory as difficult as possible.
  
  When it comes to non-memory based attacks, such as brute force attacks on our
  persisted data, we employ strong authenticated encryption and reasonable KDF
  parameters.
  
  Tactics:
  * Aggressive zeroizing
  * Using native memory (non-VM) when possible to void the GC (?)
  * Never store the password in any form on disk (even a hash)


## Key Derivation and Encryption Key Generation

The sensitive data that is cached by CacheWord can be specified by the developer as
a class implementing ICachedSecrets.

The default implementation of this class (PassphraseSecrets) attempts to
provide for most use cases. It generates a 256 bit encryption key that can be
used by other libraries like SQLCipher or IOCipher.

To initialize the secret we do the following:

1. Run the password through PBKDF2 with a random 16 byte salt
2. Generate a random 256 bit AES key with a random 96 bit IV
3. Use the derived key to encrypt the generated key in GCM mode
4. Write the ciphertext, iv, salt, and a version tag to disk ([SharedPreferences][sharedprefs])

Password verification and decryption of the AES key follows the same procedure:

1. Read the ciphertext, iv, salt, and version tag from disk
2. Run the password through PBKDF2 with the salt
3. Attempt to decrypt the ciphertext with the derived key and read iv

If the GCM operation succeeds, the password is verified and the encryption key
can be read. If the operation fails, either the password is incorrect or the
ciphertext has been modified.

TODO number of PBKDF iterations

## Managing Key Material Securely in Memory

TODO: write some bits about secrets in memory

## Official Authorities On The Use of `String`

The [Java Cryptography Architecture guide][java-crypto-arch] states,

> It would seem logical to collect and store the password in an object of type
> java.lang.String. However, here's the caveat: Objects of type String are
> immutable, i.e., there are no methods defined that allow you to change
> (overwrite) or zero out the contents of a String after usage. This feature
> makes String objects unsuitable for storing security sensitive information such
> as user passwords. You should always collect and store security sensitive
> information in a char array instead.

Yet, the [Secure Coding Guidelines for the Java Programming Language][java-secure-coding] counters,

> [...]Some transient data may be kept in mutable data structures, such as char
> arrays, and cleared immediately after use. Clearing data structures **has reduced
> effectiveness** on typical Java runtime systems *as objects are moved in memory
> transparently to the programmer.*

**Conclusion:** In Java, even char[] arrays aren't a good storage primitive.



[notecipher]: https://github.com/guardianproject/notecipher/
[sqlcipher]: https://www.zetetic.net/sqlcipher/open-source
[iocipher]: https://guardianproject.info/code/IOCipher
[issues]: https://dev.guardianproject.info/projects/cacheword/issues/new
[libguide]: http://developer.android.com/guide/developing/projects/projects-cmdline.html#ReferencingLibraryProject
[sharedprefs]: https://developer.android.com/guide/topics/data/data-storage.html#pref
[java-crypto-arch]: http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#PBEEx
[java-secure-coding]: http://www.oracle.com/technetwork/java/seccodeguide-139067.html#2
[cellibrite]: http://www.cellebrite.com
