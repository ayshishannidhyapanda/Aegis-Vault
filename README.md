# AegisVault-J

A **Java-only encrypted container system** for secure file storage.

## What This Is

AegisVault-J provides:

- **Encrypted container storage** — Files stored in a single encrypted vault file (`.avj`)
- **Virtual filesystem abstraction** — Logical file/folder operations over encrypted content
- **Cross-platform support** — Pure Java, runs on Windows, macOS, Linux
- **Strong cryptography** — AES-256-GCM encryption, Argon2id key derivation

## Current Implementation Status

| Component | Status |
|-----------|--------|
| Crypto Layer (AES-256-GCM) | ✅ Complete |
| Key Derivation (Argon2id) | ✅ Complete |
| Vault Container | ✅ Complete |
| Virtual File System | ✅ Complete |
| Vault Service | ✅ Complete |
| Unit Tests | ✅ Complete |
| JavaFX UI | ❌ Not started |

## What This Is NOT

> ⚠️ **Honest Security Disclosure**

This project does **NOT** provide:

- ❌ Disk-level or full-disk encryption
- ❌ Kernel-level filesystem security
- ❌ Forensic-grade plausible deniability
- ❌ Hidden volumes
- ❌ Protection against memory forensics on a running system
- ❌ Native filesystem mounting (no FUSE, Dokany, etc.)

This is a **Java application** that manages an encrypted container. When the vault is unlocked, decrypted data passes through JVM memory. A sufficiently privileged attacker with access to the running process could potentially extract sensitive data.

## Architecture

```
┌─────────────────────────────────────────┐
│         JavaFX UI (Optional)            │
├─────────────────────────────────────────┤
│         Vault Service Layer             │
├─────────────────────────────────────────┤
│       Virtual File System (VFS)         │
├─────────────────────────────────────────┤
│     Encrypted Container Engine          │
├─────────────────────────────────────────┤
│      Single Vault File (.avj)           │
└─────────────────────────────────────────┘
```

## Requirements

- Java 17 or higher
- BouncyCastle library (for Argon2id)

## Cryptographic Details

| Component | Choice |
|-----------|--------|
| Encryption | AES-256-GCM |
| Key Derivation | Argon2id |
| IV/Nonce | 96-bit, unique per operation |
| Auth Tag | 128-bit GCM tag |
| Randomness | `SecureRandom` |

## Building

```bash
./gradlew build
```

## Project Structure

```
src/main/java/com/aegisvault/
├── AegisVaultApplication.java    # Application entry point
├── container/                     # Encrypted container engine
├── crypto/                        # Cryptographic primitives
├── service/                       # Vault service layer
├── vfs/                          # Virtual filesystem abstraction
└── exception/                    # Custom exceptions
```

## Documentation

- [Project Context](docs/context.md) — Authoritative design document

## License

[To be determined]

## Security

If you discover a security vulnerability, please report it responsibly.

---

*Built with security honesty as a core principle.*
