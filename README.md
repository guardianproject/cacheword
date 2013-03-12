CacheWord
=========

CacheWord is an Android library project for passphrase caching and management.
It enables app developers to securely handle and store secrets derived from the
user's passphrase.

**CacheWord is still under development. DO NOT USE**

More specifically this library provides:

1. Passphrase caching: store the passphrase in memory to avoid constantly prompting the user
2. Secrets Management: strong key derivation, passphrase verifying and resetting

## Passphrase Caching

Securely cache your app's secrets in memory.

Features:

* Configurable timeout
* Manual clearing
* Uses Android's Keystore on 4.x if available

## Secrets Management

CacheWord manages key derivation, key persistence, passphrase resetting.

Features:

* Strong key derivation (using scrypt)
* Secure secret storage (AES-256 GCM)
* Uses Android's Keystore on 4.x if available

# Security Design Notes

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

[java-crypto-arch]: http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#PBEEx
[java-secure-coding]: http://www.oracle.com/technetwork/java/seccodeguide-139067.html#2


