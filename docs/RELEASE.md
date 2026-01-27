# AegisVault-J Release Information

## Current Release

| Property | Value |
|----------|-------|
| **Version** | 0.1.0 |
| **Build Date** | January 21, 2026 |
| **Release Type** | SNAPSHOT (Development) |
| **Codename** | Initial Release |

## Supported Platforms

| Platform | Architecture | Installer Type | Status |
|----------|-------------|----------------|--------|
| Windows 10/11 | x64 | .exe | ✅ Supported |
| Linux (Debian/Ubuntu) | x64 | .deb | ✅ Supported |
| Linux (RHEL/Fedora) | x64 | .rpm | ✅ Supported |
| macOS 11+ | x64 / arm64 | .dmg / .app | ✅ Supported |

## System Requirements

### Minimum Requirements

| Component | Requirement |
|-----------|-------------|
| OS | Windows 10, macOS 11, Ubuntu 20.04 or equivalent |
| RAM | 512 MB available |
| Disk Space | 200 MB for installation |
| Display | 1280x720 minimum |

### Recommended Requirements

| Component | Recommendation |
|-----------|----------------|
| RAM | 1 GB available |
| Disk Space | 500 MB+ for vaults |
| Display | 1920x1080 or higher |

## Bundled Components

| Component | Version |
|-----------|---------|
| Java Runtime | 17+ (bundled) |
| JavaFX | 17.0.2 (bundled) |
| BouncyCastle | 1.77 |

## Checksums

After building, verify checksums with:

**Windows (PowerShell):**
```powershell
Get-FileHash .\dist\windows\AegisVault-J-0.1.0.exe -Algorithm SHA256
```

**Linux/macOS:**
```bash
sha256sum dist/linux/aegisvault-j_0.1.0-1_amd64.deb
sha256sum dist/macos/AegisVault-J-0.1.0.dmg
```

## Known Issues

- macOS: Application is not signed with Apple Developer ID. Users must bypass Gatekeeper on first launch.
- Linux: Some desktop environments may require logout/login to see application in menu.

## Changelog

### Version 0.1.0 (January 21, 2026)

**Initial Release**

- Core vault encryption with AES-256-GCM
- Argon2id key derivation (64 MiB memory cost)
- JavaFX graphical user interface
- File and folder import/export
- Password strength indicator
- Auto-lock after inactivity
- Vault backup functionality
- Native installers for Windows, Linux, and macOS

## Security Notes

- All vault contents are encrypted at rest
- No network access or auto-update functionality
- Bundled runtime (no external Java required)
- See [SECURITY.md](../SECURITY.md) for full security information

---

*For installation instructions, see [README.md](../README.md)*  
*For packaging details, see [packaging.md](packaging.md)*
