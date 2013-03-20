# Security Design Notes

The goal of CacheWord is to provide easy and secure "secrets" protection for
Android developers. By "secrets" we mean sensitive key material such as
passwords and encryption keys. We specifically exclude sensitive application
data (such as the data encrypted by said keys) from secrets we aim to protect.

### Threat Model

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
4. Write the ciphertext, iv, and salt to disk ([SharedPreferences][sharedprefs])

Password verification and decryption of the AES key follows the same procedure:

1. Read the ciphertext, iv, and salt from disk
2. Run the password through PBKDF2 with a the salt
3. Attempt to decrypt the ciphertext with the derived key and read iv

If the GCM operation succeeds, the password is verified and the encryption key
can be read. If the operation fails, either the password is incorrect or the
ciphertext has been modified.

TODO number of PBKDF iterations

## Managing Key Material Securely in Memory

TODO: write some bits about secrets in memory

### Official Authorities On The Use of `String`

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

[sharedprefs]: https://developer.android.com/guide/topics/data/data-storage.html#pref
[java-crypto-arch]: http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#PBEEx
[java-secure-coding]: http://www.oracle.com/technetwork/java/seccodeguide-139067.html#2
[cellibrite]: http://www.cellebrite.com


