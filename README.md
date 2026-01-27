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

## Installation

AegisVault-J is distributed as a native application for Windows, Linux, and macOS.  
**No Java installation is required** — the application includes its own bundled runtime.

### Windows

1. Download `AegisVault-J-0.1.0.exe` from the releases page
2. Run the installer
3. Follow the installation wizard
4. Launch from Start Menu → AegisVault-J

**What gets installed:**
- Application in `C:\Program Files\AegisVault-J\`
- Start Menu shortcut
- Optional desktop shortcut

### Linux

**Debian/Ubuntu (.deb):**
```bash
# Download and install
sudo apt install ./aegisvault-j_0.1.0-1_amd64.deb

# Or using dpkg
sudo dpkg -i aegisvault-j_0.1.0-1_amd64.deb
```

**RHEL/Fedora (.rpm):**
```bash
sudo dnf install ./aegisvault-j-0.1.0-1.x86_64.rpm
```

**After installation:**
- Launch from Applications menu → AegisVault-J
- Or run `aegisvault-j` from terminal

### macOS

1. Download `AegisVault-J-0.1.0.dmg` from the releases page
2. Open the DMG file
3. Drag AegisVault-J to Applications folder
4. Launch from Applications

**⚠️ First Launch (Gatekeeper):**

The application is not signed with an Apple Developer ID. On first launch:
1. Right-click on AegisVault-J.app
2. Select "Open" from the context menu
3. Click "Open" in the security dialog

Or: System Settings → Privacy & Security → Click "Open Anyway"

---

## Quick Start

### Running from Source (Development)

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
| Auto-Lock | Automatic lock after configurable inactivity timeout |
| Change Password | Update vault password |
| Backup Vault | Create copies of vault file |
| Recent Vaults | Quick access to recently opened vaults |
| Search & Filter | Find files quickly within the vault |
| Multi-Select | Select multiple files for bulk operations |
| Progress Indicators | Visual feedback for long operations |

---

## User Experience

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+N | Create new vault |
| Ctrl+O | Open vault |
| Ctrl+L | Close/Lock vault |
| Ctrl+Q | Exit application |
| Ctrl+I | Import file |
| Ctrl+Shift+I | Import folder |
| Ctrl+E | Export selected |
| Ctrl+D | New folder |
| Ctrl+B | Backup vault |
| Ctrl+F | Search files |
| F5 | Refresh file list |
| F1 | About |
| Delete | Delete selected |
| Backspace | Go to parent folder |
| Enter | Open folder |

### Progress Feedback

Long operations like importing folders or exporting multiple files show progress dialogs with:
- Visual progress bar
- Current file being processed
- Cancel button (where safe)

### Auto-Lock

Configure automatic vault locking via Settings → Auto-Lock Timeout. Options: 1, 5, 10, 15, 30, or 60 minutes. Your setting is remembered between sessions.

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

## Building from Source

### Development Build

```bash
./gradlew build
```

### Creating Native Installers

AegisVault-J uses **jpackage** to create native installers with bundled JVM runtime.

**Windows:**
```powershell
# Requires WiX Toolset 3.x
.\gradlew.bat packageWindows
# Output: dist/windows/AegisVault-J-0.1.0.exe
```

**Linux:**
```bash
# Debian/Ubuntu (requires dpkg-dev)
./gradlew packageLinuxDeb
# Output: dist/linux/aegisvault-j_0.1.0-1_amd64.deb

# RHEL/Fedora (requires rpm-build)
./gradlew packageLinuxRpm
# Output: dist/linux/aegisvault-j-0.1.0-1.x86_64.rpm
```

**macOS:**
```bash
# Requires Xcode command line tools
./gradlew packageMacOs
# Output: dist/macos/AegisVault-J-0.1.0.dmg
```

For detailed packaging instructions, see [Packaging Guide](docs/packaging.md).

## Build Requirements

### For Development

| Requirement | Version |
|-------------|---------|
| JDK | 17+ |
| Gradle | 8+ (wrapper included) |

### For Native Packaging

| Platform | Additional Requirement |
|----------|------------------------|
| Windows | WiX Toolset 3.x |
| Linux (DEB) | dpkg-dev, fakeroot |
| Linux (RPM) | rpm-build |
| macOS | Xcode command line tools |

---

## Documentation

- [Project Context](docs/context.md) — Authoritative design document
- [Security Policy](SECURITY.md) — Threat model and security details
- [Security Audit](docs/SECURITY_AUDIT.md) — Phase A audit results
- [Packaging Guide](docs/packaging.md) — Native installer build instructions
- [Release Information](docs/RELEASE.md) — Version and platform details
- [Experimental Crypto](docs/experimental-crypto.md) — Advanced cryptographic options

---

## ⚠️ Advanced Users: Experimental Features

AegisVault-J includes an **experimental cryptography framework** for security researchers and advanced users. These features are:

| Feature | Default State | Risk |
|---------|---------------|------|
| Alternative Ciphers (Serpent, Twofish, Camellia) | DISABLED | High |
| Cipher Cascades | DISABLED | Very High |
| Alternative KDFs | DISABLED | High |
| Mouse Entropy Collection | DISABLED | Low |

### ⚠️ Important Warnings

1. **All experimental features are DISABLED by default**
2. **The security audit does NOT apply when experimental features are enabled**
3. **Do NOT use experimental modes for sensitive or production data**
4. **Experimental features may introduce vulnerabilities**

### Enabling Experimental Features

Only for research/testing purposes:

```bash
java -Daegisvault.experimental.ciphers.enabled=true \
     -Daegisvault.experimental.cipher.serpent.enabled=true \
     -jar aegis-vault.jar
```

See [docs/experimental-crypto.md](docs/experimental-crypto.md) for complete documentation.

### Recommended Configuration

For **all production use**, use the defaults:
- Cipher: **AES-256-GCM** (audited)
- KDF: **Argon2id** (audited)
- No experimental features

---

## Release Information

| Property | Value |
|----------|-------|
| Version | 0.1.0 |
| Build Date | January 21, 2026 |
| Platforms | Windows, Linux, macOS |
| Architecture | x64 |
| Bundled Runtime | Java 17+ |

---

## License

Copyright (c) 2026 Aegis Vault. All rights reserved.

See source files for full license terms.

If you discover a security vulnerability, please report it responsibly.

---

*Built with security honesty as a core principle.*
