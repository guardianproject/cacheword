# Security Design Notes

## Key Derivation and Encryption Key Generation

The sensitive data that is cached by CacheWord can be specified by the user as
a class implementing ICachedSecrets.

The default implementation of this class (PassphraseSecrets) attempts to
provide for most use cases. It generates a 256 bit encryption key that can be
used by other libraries like SQLCipher or IOCipher.

To initialize the secret we do the following:

1. Run the password through PBKDF2 with a random 16 byte salt
2. Generate a random 256 bit AES key with a random 96 bit IV
3. Use the derived key to encrypt the generated key in GCM mode
4. Write the ciphertext, iv, and salt to disk ([SharedPreferences][sharedprefs])

Password verification and decryption of the AES key follow the same procedure:

1. Read the ciphertext, iv, and salt from disk
2. Run the password through PBKDF2 with a the salt
3. Attempt to decrypt the ciphertext with the derived key and read iv

If the GCM operation succeeds, the password is verified and the encryption key
can be read.

TODO number of PBKDF iterations

## Managing Key Material Securely

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


