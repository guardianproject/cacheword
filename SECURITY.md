# Security Design Notes

## Key Derivation and Encryption Key Generation

TODO: copy documenttion of the crypto in PassphraseSecrets.java to here

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


