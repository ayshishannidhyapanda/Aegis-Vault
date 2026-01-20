# AegisVault-J — Project Context (Living Document)

> **Last Updated:** 2026-01-20  
> **Status:** Active Development  
> **This file is the single source of truth for all architectural and security decisions.**

---

## 1. Project Purpose

AegisVault-J is a **Java-only encrypted container system** that provides:

- Secure storage of files within a single encrypted vault file (`.avj`)
- A logical/virtual filesystem abstraction over encrypted content
- Cross-platform compatibility (Windows, macOS, Linux)
- Strong cryptographic guarantees using modern algorithms

---

## 2. Explicit Constraints (FROZEN)

| Constraint | Value | Rationale |
|------------|-------|-----------|
| Language | Java 17+ | Modern LTS, strong crypto support |
| Native Code | **PROHIBITED** | No JNI, no C/C++, no native libraries |
| Kernel Integration | **PROHIBITED** | No FUSE, Dokany, WinFsp, OS drivers |
| Encryption Algorithm | AES-256-GCM | Authenticated encryption standard |
| Key Derivation | Argon2id (BouncyCastle) | Memory-hard, GPU-resistant |
| Integrity Verification | GCM authentication tag | Built into cipher mode |
| Randomness Source | `java.security.SecureRandom` | Cryptographically secure |
| Password Handling | `char[]` only | Never `String` (immutable, cannot be wiped) |
| Copyright Header | **REQUIRED** | All Java files must include copyright |
| Code Comments | **PROHIBITED** | No inline/block comments in code |
| Password Handling | `char[]` only | Never `String` (immutable, cannot be wiped) |

---

## 3. Cryptography Choices

### 3.1 Encryption
- **Algorithm:** AES-256-GCM
- **Key Size:** 256 bits
- **IV/Nonce:** 96 bits (12 bytes), unique per encryption operation
- **Authentication Tag:** 128 bits (16 bytes)

### 3.2 Key Derivation
- **Algorithm:** Argon2id
- **Library:** BouncyCastle
- **Parameters:** (To be tuned based on target hardware)
  - Memory: 64 MiB minimum
  - Iterations: 3 minimum
  - Parallelism: 1
- **Salt:** 256 bits (32 bytes), randomly generated per vault

### 3.3 Master Key Architecture
- User password → Argon2id → Master Key (256 bits)
- Master Key encrypts Vault Key
- Vault Key encrypts file content keys
- File Content Keys encrypt individual file data

---

## 4. Threat Model

### 4.1 In Scope (Protected Against)
- Unauthorized access to vault file contents
- Brute-force password attacks (mitigated by Argon2id)
- Data tampering detection (GCM authentication)
- Password recovery from memory (char[] zeroing)

### 4.2 Out of Scope (NOT Protected Against)
- Malware with memory access while vault is unlocked
- Keyloggers capturing password entry
- Physical attacks on running system
- Forensic analysis of swap/hibernation files
- Side-channel attacks on the JVM
- Compromised JVM or OS

### 4.3 Explicit Non-Claims
This project **does NOT provide**:
- Disk-level encryption
- Kernel-level filesystem security
- Forensic-grade plausible deniability
- Hidden volumes
- Protection against memory forensics

---

## 5. Architecture Layers

```
┌─────────────────────────────────────────┐
│         JavaFX UI (Optional)            │  ← Presentation only
├─────────────────────────────────────────┤
│         Vault Service Layer             │  ← Orchestration, business logic
├─────────────────────────────────────────┤
│       Virtual File System (VFS)         │  ← Logical file/folder abstraction
├─────────────────────────────────────────┤
│     Encrypted Container Engine          │  ← Crypto operations, block I/O
├─────────────────────────────────────────┤
│      Single Vault File (.avj)           │  ← Physical storage
└─────────────────────────────────────────┘
```

### Layer Responsibilities

| Layer | Responsibility | Security Boundary |
|-------|---------------|-------------------|
| UI | User interaction, display | Never handles keys |
| Vault Service | Coordinates operations | Holds unlocked vault reference |
| VFS | Logical file operations | Operates on decrypted streams |
| Container Engine | Encryption/decryption | Manages all key material |
| Vault File | Persistent storage | Encrypted at rest |

---

## 6. Terminology Rules

### MUST Use
- "Encrypted container"
- "Logical filesystem" or "Virtual filesystem"
- "Java-based secure vault"
- "Vault file"

### MUST NOT Use
- "Disk encryption"
- "Full disk encryption"
- "Kernel-level security"
- "Forensic-proof"
- "Uncrackable"
- "Military-grade" (marketing term)

---

## 7. Non-Goals

The following are explicitly **out of scope**:

1. ~~Native filesystem mounting~~ — No FUSE/Dokany integration
2. ~~Cloud sync~~ — Local-only operation
3. ~~Multi-user concurrent access~~ — Single-user vault model
4. ~~Hidden/plausible deniability volumes~~ — Not claimed or implemented
5. ~~Key escrow or recovery~~ — Lost password = lost data
6. ~~Compression~~ — Security over space efficiency (compression oracle attacks)
7. ~~Steganography~~ — Out of scope

---

## 8. Frozen Decisions

These decisions are **immutable** unless a critical security flaw is discovered:

| Decision | Date | Rationale |
|----------|------|-----------|
| AES-256-GCM for encryption | 2026-01-20 | Industry standard AEAD |
| Argon2id for KDF | 2026-01-20 | Winner of Password Hashing Competition |
| No native code | 2026-01-20 | Cross-platform, auditable |
| Single vault file format | 2026-01-20 | Simplicity, portability |
| char[] for passwords | 2026-01-20 | Memory safety |
| BouncyCastle for Argon2 | 2026-01-20 | Pure Java implementation |
| Copyright header required | 2026-01-20 | Legal protection, all Java files |
| No code comments | 2026-01-20 | Clean code, self-documenting |

---

## 9. Known Limitations

### 9.1 JVM Limitations
- Memory cannot be guaranteed zeroed (GC unpredictability)
- No control over swap/page file
- JIT compilation may copy sensitive data

### 9.2 Design Limitations
- Maximum vault size limited by filesystem (no chunking planned)
- No incremental backup support
- No journaling (crash during write may corrupt)
- Performance bounded by Java I/O

### 9.3 Security Limitations
- Unlocked vault keys reside in JVM heap
- No hardware security module (HSM) support
- No secure enclave integration

---

## 10. File Format (.avj)

> **Status:** Draft specification

```
[Header: 64 bytes]
  - Magic bytes: "AEGISVLT" (8 bytes)
  - Format version: uint16
  - Flags: uint16
  - Salt: 32 bytes
  - IV for header encryption: 12 bytes
  - Reserved: 10 bytes

[Encrypted Metadata Block]
  - Vault key (encrypted with master key)
  - File index (encrypted)

[Encrypted Data Blocks]
  - Variable-size encrypted file content
```

---

## 11. Dependencies

| Dependency | Purpose | Version |
|------------|---------|---------|
| BouncyCastle | Argon2id implementation | 1.77+ |
| JavaFX | Optional UI | 17+ |

---

## 12. Change Log

| Date | Change | Author |
|------|--------|--------|
| 2026-01-20 | Initial context document created | System |
| 2026-01-20 | Added copyright header requirement, no-comments policy | System |
| 2026-01-20 | Implemented AesGcmCipher with full AES-256-GCM encryption/decryption | System |
| 2026-01-20 | Implemented Argon2KeyDeriver with BouncyCastle Argon2id | System |
| 2026-01-20 | Added comprehensive unit tests for crypto layer | System |
| 2026-01-20 | Implemented VaultHeader with serialization/deserialization | System |
| 2026-01-20 | Implemented VaultContainer with create/open/close, file I/O, password change | System |
| 2026-01-20 | Implemented VirtualFileSystem with full directory/file operations | System |
| 2026-01-20 | Implemented VaultService as orchestration layer | System |
| 2026-01-20 | Added comprehensive tests for container, VFS, and service layers | System |

---

## 13. Validation Checklist

Before any PR/commit, verify:

- [ ] Does this change comply with frozen decisions?
- [ ] Does this change maintain the threat model boundaries?
- [ ] Does this change use correct terminology?
- [ ] Has context.md been updated if a new decision was made?
- [ ] Are there any new limitations to document?

---

*This document is authoritative. All code must conform to it.*
