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

### 1.1 What AegisVault-J IS

AegisVault-J is a **digital safe** — an application that:

- Creates a single encrypted file (`.avj`) that acts as a secure container
- Allows users to **import** files into the encrypted container
- Stores all imported files in encrypted form inside the vault
- Allows users to **export** files to a chosen location when needed
- Requires a password to unlock and access the contents

**Analogy:** Think of it like a physical safe. You put documents inside, lock it, and they're protected. To use a document, you unlock the safe, take it out, use it, and put it back when done.

### 1.2 What AegisVault-J IS NOT

AegisVault-J is **NOT**:

| It is NOT | Explanation |
|-----------|-------------|
| A mounted filesystem | Files are not accessible as a drive letter or folder |
| A live encrypted folder | You cannot open files directly from within the vault |
| Disk encryption | It does not encrypt your entire hard drive |
| A transparent encryption layer | Files must be explicitly exported to use |
| A replacement for BitLocker/FileVault | Those are OS-level disk encryption tools |

---

## 2. Usage Model (FROZEN)

### 2.1 The Import-Export Security Model

AegisVault-J follows a deliberate **Import → Encrypt → Lock → Export → Use → Delete → Re-Import** workflow:

```
┌─────────────────────────────────────────────────────────────────┐
│                     SECURE WORKFLOW                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   [External File]                                                │
│         │                                                        │
│         ▼                                                        │
│   ┌─────────────┐                                                │
│   │   IMPORT    │  ← File is encrypted and stored in vault      │
│   └─────────────┘                                                │
│         │                                                        │
│         ▼                                                        │
│   ┌─────────────────────────────────────┐                        │
│   │     ENCRYPTED VAULT (.avj)          │  ← Safe at rest       │
│   │  ┌───────────────────────────────┐  │                        │
│   │  │ Encrypted File 1              │  │                        │
│   │  │ Encrypted File 2              │  │                        │
│   │  │ Encrypted File 3              │  │                        │
│   │  └───────────────────────────────┘  │                        │
│   └─────────────────────────────────────┘                        │
│         │                                                        │
│         ▼                                                        │
│   ┌─────────────┐                                                │
│   │   EXPORT    │  ← File is decrypted to chosen location       │
│   └─────────────┘                                                │
│         │                                                        │
│         ▼                                                        │
│   [Temporary Decrypted File]  ← USER'S RESPONSIBILITY           │
│         │                                                        │
│         ▼                                                        │
│   ┌─────────────┐                                                │
│   │    USE      │  ← Open with external application             │
│   └─────────────┘                                                │
│         │                                                        │
│         ▼                                                        │
│   ┌─────────────┐                                                │
│   │   DELETE    │  ← User deletes the exported file             │
│   └─────────────┘                                                │
│         │                                                        │
│         ▼                                                        │
│   ┌─────────────┐                                                │
│   │  RE-IMPORT  │  ← If modified, import updated version        │
│   └─────────────┘                                                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Why This Model?

**This is intentional and more secure for a Java-only system:**

| Reason | Explanation |
|--------|-------------|
| **No kernel access** | Java cannot create virtual drives or mount filesystems without native code (FUSE, Dokany). This is a frozen constraint. |
| **Clear security boundary** | The vault file is the security boundary. Once a file is exported, it leaves the protected zone. |
| **User awareness** | Users explicitly see when files enter/leave the vault, promoting security consciousness. |
| **No hidden temp files** | Unlike transparent encryption, there are no hidden decrypted copies the user doesn't know about. |
| **Portability** | The vault file can be copied, backed up, or moved without special handling. |

### 2.3 User Workflow

#### Step 1: Create a Vault
- User creates a new vault file (`.avj`)
- User sets a strong password
- Empty vault is created and locked

#### Step 2: Unlock the Vault
- User opens an existing vault file
- User enters the password
- Vault is unlocked and contents are viewable (as a list, not as files)

#### Step 3: Import Files
- User selects files/folders from their system
- Files are encrypted and stored inside the vault
- Original files remain unchanged (user may delete them)

#### Step 4: View Contents
- User sees a logical list of files/folders inside the vault
- This is NOT a mounted drive — just a list view
- Files cannot be "opened" directly from this view

#### Step 5: Export Files
- User selects files to export
- User chooses a destination folder
- Files are decrypted and written to the destination
- **WARNING:** Exported files are unprotected

#### Step 6: Use Exported Files
- User opens exported files with appropriate applications
- These files are outside the vault's protection
- User is responsible for their security

#### Step 7: Clean Up
- User deletes exported files when done
- If files were modified, user re-imports them to the vault

#### Step 8: Lock the Vault
- User closes the vault
- Vault is locked and all keys are wiped from memory
- Vault file remains encrypted on disk

---

## 3. Security Boundaries (FROZEN)

### 3.1 What is Protected

| Scenario | Protection Level |
|----------|------------------|
| Vault file at rest (locked) | ✅ **PROTECTED** — Encrypted with AES-256-GCM |
| Vault file copied to USB | ✅ **PROTECTED** — Still encrypted |
| Vault file sent via email | ✅ **PROTECTED** — Still encrypted |
| Computer stolen (vault locked) | ✅ **PROTECTED** — Attacker cannot read contents |
| Brute-force password attack | ✅ **PROTECTED** — Argon2id makes this extremely slow |

### 3.2 What is NOT Protected

| Scenario | Protection Level |
|----------|------------------|
| Vault unlocked, attacker has system access | ❌ **NOT PROTECTED** — Keys in memory |
| Exported files on disk | ❌ **NOT PROTECTED** — User's responsibility |
| Keylogger captures password | ❌ **NOT PROTECTED** — Application-level threat |
| Malware reads JVM memory | ❌ **NOT PROTECTED** — Runtime threat |
| Swap file contains decrypted data | ❌ **NOT PROTECTED** — OS-level, Java cannot control |
| User shares their password | ❌ **NOT PROTECTED** — Human factor |

### 3.3 Security Boundary Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    OUTSIDE SECURITY BOUNDARY                     │
│                                                                  │
│   • Exported files                                               │
│   • User's password knowledge                                    │
│   • Running system memory (while unlocked)                       │
│   • Clipboard contents                                           │
│   • Screen contents                                              │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                    INSIDE SECURITY BOUNDARY                      │
│                                                                  │
│   • Vault file contents (when locked)                            │
│   • Encrypted file data                                          │
│   • Encrypted metadata                                           │
│   • Vault key (encrypted with master key)                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. UI Behavior Rules (FROZEN)

### 4.1 Vault Contents Display

- Vault contents are displayed as a **logical list**, not as OS files
- Users see file names, sizes, and dates — not actual files
- Double-clicking a file does NOT open it (directories navigate, files show info)
- There is no "Open" action that directly launches files

### 4.2 Export Behavior

- Export always requires user to choose a destination
- UI MUST display a warning: "Exported files are not protected by the vault"
- Exported files are the user's responsibility
- Application does NOT track or manage exported files

### 4.3 Import Behavior

- Import copies files into the vault (does not move)
- Original files remain unchanged
- User should manually delete originals if desired

### 4.4 Lock Behavior

- Locking clears all keys from memory
- Auto-lock triggers after configurable inactivity
- UI returns to locked state immediately

---

## 5. Explicit Constraints (FROZEN)

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
| 2026-01-20 | Implemented JavaFX UI layer with MainApplication, MainController | System |
| 2026-01-20 | Added PasswordDialog and ChangePasswordDialog components | System |
| 2026-01-20 | Added ImportExportUtil for file/folder import and export | System |
| 2026-01-20 | Added BackupUtil for vault backup operations | System |
| 2026-01-20 | Created CSS stylesheet for UI styling | System |
| 2026-01-20 | Added PasswordStrength utility with entropy calculation | System |
| 2026-01-20 | Enhanced PasswordDialog with visual strength indicator | System |
| 2026-01-20 | Added auto-lock feature with configurable inactivity timeout | System |
| 2026-01-20 | Added tests for ImportExportUtil and BackupUtil | System |
| 2026-01-20 | Created SECURITY.md with threat model documentation | System |
| 2026-01-20 | Added Usage Model section defining import-export workflow | System |
| 2026-01-20 | Added Security Boundaries section with clear protection scope | System |
| 2026-01-20 | Added UI Behavior Rules for consistent user experience | System |
| 2026-01-20 | Clarified what AegisVault-J IS and IS NOT | System |
| 2026-01-20 | Added export security warning dialog in UI | System |
| 2026-01-20 | Added auto-lock notification callback | System |
| 2026-01-20 | Updated welcome screen with product description | System |
| 2026-01-20 | Created PROJECT_STATUS.md with complete summary | System |
| 2026-01-20 | Completed Phase A security audit — PASSED | Security |
| 2026-01-20 | Created SECURITY_AUDIT.md with detailed findings | Security |
| 2026-01-20 | Updated SECURITY.md with PasswordField limitation disclosure | Security |
| 2026-01-21 | Phase B: Added ProgressDialog component for async operations | UX |
| 2026-01-21 | Phase B: Import files/folders now shows progress with cancel option | UX |
| 2026-01-21 | Phase B: Export multiple items shows progress with cancel option | UX |
| 2026-01-21 | Phase B: Backup vault now shows progress indicator | UX |
| 2026-01-21 | Phase B: Enhanced export warning dialog with clearer emphasis | UX |
| 2026-01-21 | Phase B: Added export completion reminder about deleting files | UX |
| 2026-01-21 | Phase B: Improved status messages with success indicators | UX |
| 2026-01-21 | Phase B: Added progress bar and disabled button CSS styles | UX |
| 2026-01-21 | Phase B: Updated README.md with UX features and shortcuts | UX |
| 2026-01-21 | Phase C: Added jpackage tasks for Windows .exe installer | Packaging |
| 2026-01-21 | Phase C: Added jpackage tasks for Linux .deb and .rpm packages | Packaging |
| 2026-01-21 | Phase C: Added jpackage tasks for macOS .dmg and .app bundle | Packaging |
| 2026-01-21 | Phase C: Created packaging/icons directory with SVG source icon | Packaging |
| 2026-01-21 | Phase C: Created packaging/linux/aegisvault-j.desktop entry | Packaging |
| 2026-01-21 | Phase C: Created docs/packaging.md with detailed build instructions | Packaging |
| 2026-01-21 | Phase C: Created docs/RELEASE.md with version and platform info | Packaging |
| 2026-01-21 | Phase C: Updated README.md with Installation section per OS | Packaging |
| 2026-01-21 | Phase C: Created dist/ directory structure for build artifacts | Packaging |
| 2026-01-21 | Phase C: Updated build.gradle with modern Gradle task syntax | Packaging |
| 2026-01-21 | Phase E: Created docs/experimental-crypto.md design document | Experimental |
| 2026-01-21 | Phase E: Created CipherProvider interface for pluggable ciphers | Experimental |
| 2026-01-21 | Phase E: Created AesGcmCipherProvider wrapping audited implementation | Experimental |
| 2026-01-21 | Phase E: Created ExperimentalCipherProvider abstract base class | Experimental |
| 2026-01-21 | Phase E: Created CipherRegistry with experimental feature flags | Experimental |
| 2026-01-21 | Phase E: Created KeyDerivationFunction interface | Experimental |
| 2026-01-21 | Phase E: Created Argon2idKdfProvider wrapping audited KDF | Experimental |
| 2026-01-21 | Phase E: Created CascadeCipherProvider skeleton (design only) | Experimental |
| 2026-01-21 | Phase E: Created MouseEntropyCollector skeleton | Experimental |
| 2026-01-21 | Phase E: Created CryptoSelfTest for startup validation | Experimental |
| 2026-01-21 | Phase E: Created ExperimentalConfig for feature flag management | Experimental |
| 2026-01-21 | Phase E: Updated SECURITY.md with experimental features warning | Experimental |
| 2026-01-21 | Phase E: Updated README.md with advanced users warning section | Experimental |

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
