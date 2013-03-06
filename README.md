CacheWord
=========

CacheWord is an Android library project for passphrase caching and management.
It enables app developers to securely handle and store secrets derived from the
user's passphrase.

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

