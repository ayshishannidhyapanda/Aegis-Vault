# AegisVault-J

A **Java-only encrypted container system** for secure file storage.

---

## What is AegisVault-J?

**AegisVault-J is a digital safe for your files.**

Think of it like a physical safe:
- You **put documents inside** the safe (import)
- The safe **locks them securely** (encryption)
- When you need a document, you **take it out** (export)
- While outside the safe, **you're responsible** for the document
- When done, you **put it back** (re-import) or destroy it

### Simple Explanation

| Action | What Happens |
|--------|--------------|
| **Create Vault** | Creates a new encrypted container file (`.avj`) |
| **Unlock** | Enter your password to access the vault |
| **Import** | Copy files INTO the vault (they get encrypted) |
| **Browse** | See a list of what's in your vault |
| **Export** | Copy files OUT of the vault (they get decrypted) |
| **Lock** | Close the vault, wipe keys from memory |

---

## ⚠️ Important: How This Works

### Files are NOT opened directly from the vault

When you want to use a file:
1. **Export** the file to a folder on your computer
2. **Open** the exported file with your normal applications
3. **Delete** the exported file when you're done
4. If you made changes, **re-import** the updated file

### Why this design?

This is **intentional and more secure**:

- ✅ You always know when files leave the protected vault
- ✅ No hidden temporary files you don't know about  
- ✅ The vault file can be safely copied, backed up, or moved
- ✅ Works on any platform without special drivers

---

## ⚠️ Security Disclaimers

### What IS Protected

| Scenario | Protected? |
|----------|------------|
| Vault file sitting on your disk (locked) | ✅ YES |
| Vault file copied to a USB drive | ✅ YES |
| Computer stolen while vault is locked | ✅ YES |
| Someone tries to guess your password | ✅ YES (very slow) |

### What is NOT Protected

| Scenario | Protected? |
|----------|------------|
| **Exported files on your disk** | ❌ NO — Your responsibility |
| Vault is unlocked and attacker has access | ❌ NO |
| Malware on your computer | ❌ NO |
| Someone watches you type your password | ❌ NO |
| You forget your password | ❌ NO — Data is lost forever |

### Exported Files Warning

> **⚠️ IMPORTANT:** When you export files from the vault, they are **no longer protected**.
>
> Exported files are regular, unencrypted files. Anyone with access to your computer can read them.
>
> **Delete exported files when you're done using them.**

---

## What This Is NOT

AegisVault-J does **NOT**:

| Feature | Status |
|---------|--------|
| Encrypt your entire hard drive | ❌ NOT provided |
| Create a virtual drive letter | ❌ NOT provided |
| Let you open files directly | ❌ NOT provided |
| Protect against malware | ❌ NOT provided |
| Protect you if vault is unlocked | ❌ NOT provided |
| Recover forgotten passwords | ❌ NOT provided |

This is a **Java application**, not a disk encryption tool. For full-disk encryption, use BitLocker (Windows), FileVault (macOS), or LUKS (Linux).

---

## Quick Start

### Running the Application

```bash
./gradlew run
```

### Workflow

1. **Create a vault:** File → New Vault → Choose location → Set password
2. **Import files:** Vault → Import File/Folder → Select files
3. **Browse contents:** View your encrypted files in the list
4. **Export when needed:** Select file → Vault → Export → Choose destination
5. **Lock when done:** File → Close Vault

---

## Features

| Feature | Description |
|---------|-------------|
| Create/Open Vaults | Password-protected encrypted containers |
| Import Files/Folders | Add files to the vault (encrypted) |
| Export Files/Folders | Extract files from the vault (decrypted) |
| Browse Contents | View logical file/folder list |
| Password Strength | Visual indicator when setting password |
| Auto-Lock | Automatic lock after 15 minutes of inactivity |
| Change Password | Update vault password |
| Backup Vault | Create copies of vault file |

---

## Technical Details

| Component | Choice |
|-----------|--------|
| Encryption | AES-256-GCM |
| Key Derivation | Argon2id (memory-hard, GPU-resistant) |
| Platform | Pure Java 17+, cross-platform |
| Native Code | None (no FUSE, no drivers) |

---

## Implementation Status

| Component | Status |
|-----------|--------|
| Encryption Layer | ✅ Complete |
| Key Derivation | ✅ Complete |
| Vault Container | ✅ Complete |
| Virtual File System | ✅ Complete |
| JavaFX UI | ✅ Complete |
| Import/Export | ✅ Complete |
| Auto-Lock | ✅ Complete |

---

## Building

```bash
./gradlew build
```

## Requirements

- Java 17 or higher
- No additional software required

---

## Documentation

- [Project Context](docs/context.md) — Authoritative design document
- [Security Policy](SECURITY.md) — Threat model and security details

---

## License

Copyright (c) 2026 Aegis Vault. All rights reserved.

See source files for full license terms.

If you discover a security vulnerability, please report it responsibly.

---

*Built with security honesty as a core principle.*
