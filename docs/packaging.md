# AegisVault-J — Packaging Guide

> **Document Version:** 1.0  
> **Last Updated:** January 21, 2026  
> **Target Platforms:** Windows, Linux, macOS

---

## Overview

This document provides complete instructions for building native installers for AegisVault-J using **jpackage** (included in JDK 14+). The goal is to produce platform-native applications that:

- Run without requiring users to install Java
- Include all dependencies and a bundled JVM runtime
- Integrate naturally with each platform (icons, shortcuts, file associations)

---

## Build Prerequisites

### All Platforms

| Requirement | Version | Notes |
|-------------|---------|-------|
| JDK | 17+ | Must include jpackage (JDK 14+) |
| Gradle | 8.0+ | Wrapper included in project |
| Git | Any | For cloning repository |

### Platform-Specific Requirements

#### Windows

| Requirement | Version | Installation |
|-------------|---------|--------------|
| WiX Toolset | 3.11+ | Download from https://wixtoolset.org/releases/ |
| Windows SDK | 10+ | Optional, for code signing |

**WiX Installation:**
1. Download WiX Toolset v3.11+ from https://github.com/wixtoolset/wix3/releases
2. Run the installer
3. Add WiX bin directory to PATH (typically `C:\Program Files (x86)\WiX Toolset v3.11\bin`)
4. Verify: `candle.exe -?` should show help

#### Linux

| Requirement | Installation (Debian/Ubuntu) | Installation (RHEL/Fedora) |
|-------------|------------------------------|----------------------------|
| dpkg-deb | `sudo apt install dpkg-dev` | N/A |
| rpmbuild | N/A | `sudo dnf install rpm-build` |
| fakeroot | `sudo apt install fakeroot` | `sudo dnf install fakeroot` |

#### macOS

| Requirement | Installation |
|-------------|--------------|
| Xcode CLI Tools | `xcode-select --install` |
| Developer ID (optional) | For code signing (Apple Developer account) |

---

## Quick Start

### Build for Current Platform

```bash
# Build the application JAR first
./gradlew build

# Package for your current OS
./gradlew packageCurrentPlatform
```

### View Packaging Info

```bash
./gradlew printPackagingInfo
```

---

## Windows Packaging

### Building the Windows Installer

```powershell
# Ensure WiX Toolset is installed and in PATH
candle.exe -?

# Build the .exe installer
.\gradlew.bat packageWindows
```

### Output Location

```
dist/windows/AegisVault-J-0.1.0.exe
```

### What Gets Installed

| Component | Location |
|-----------|----------|
| Application | `C:\Program Files\AegisVault-J\` |
| Start Menu Entry | `Start Menu\Programs\AegisVault-J\` |
| Desktop Shortcut | Optional (user choice during install) |
| Bundled JRE | `C:\Program Files\AegisVault-J\runtime\` |

### Manual jpackage Command

If you need to run jpackage manually:

```powershell
# From project root, after ./gradlew build
jpackage `
  --type exe `
  --name "AegisVault-J" `
  --app-version 0.1.0 `
  --vendor "Aegis Vault" `
  --description "Secure encrypted container for your files" `
  --copyright "Copyright (c) 2026 Aegis Vault" `
  --input build/libs `
  --main-jar aegis-vault-0.1.0-SNAPSHOT.jar `
  --main-class com.aegisvault.AegisVaultApplication `
  --dest dist/windows `
  --java-options "-Xmx512m" `
  --win-dir-chooser `
  --win-menu `
  --win-menu-group "AegisVault-J" `
  --win-shortcut `
  --win-shortcut-prompt `
  --icon packaging/icons/aegisvault-j.ico `
  --module-path "build/libs/dependencies"
```

### Adding a Custom Icon

1. Create a 256x256 pixel icon
2. Convert to `.ico` format (must include 16x16, 32x32, 48x48, 256x256 sizes)
3. Place at `packaging/icons/aegisvault-j.ico`
4. Rebuild

**Icon Conversion (using ImageMagick):**
```powershell
magick convert aegisvault-j.png -define icon:auto-resize=256,128,64,48,32,16 aegisvault-j.ico
```

---

## Linux Packaging

### Building DEB Package (Debian/Ubuntu)

```bash
# Ensure dpkg-dev is installed
sudo apt install dpkg-dev fakeroot

# Build the .deb package
./gradlew packageLinuxDeb
```

### Building RPM Package (RHEL/Fedora)

```bash
# Ensure rpm-build is installed
sudo dnf install rpm-build

# Build the .rpm package
./gradlew packageLinuxRpm
```

### Output Locations

```
dist/linux/aegisvault-j_0.1.0-1_amd64.deb
dist/linux/aegisvault-j-0.1.0-1.x86_64.rpm
```

### Installation

**DEB:**
```bash
sudo dpkg -i dist/linux/aegisvault-j_0.1.0-1_amd64.deb
# Or with dependencies:
sudo apt install ./dist/linux/aegisvault-j_0.1.0-1_amd64.deb
```

**RPM:**
```bash
sudo rpm -i dist/linux/aegisvault-j-0.1.0-1.x86_64.rpm
# Or with DNF:
sudo dnf install ./dist/linux/aegisvault-j-0.1.0-1.x86_64.rpm
```

### What Gets Installed

| Component | Location |
|-----------|----------|
| Application | `/opt/aegisvault-j/` |
| Executable | `/opt/aegisvault-j/bin/aegisvault-j` |
| Desktop Entry | `/usr/share/applications/aegisvault-j.desktop` |
| Icon | `/opt/aegisvault-j/lib/aegisvault-j.png` |
| Bundled JRE | `/opt/aegisvault-j/lib/runtime/` |

### Manual jpackage Command

```bash
# From project root, after ./gradlew build
jpackage \
  --type deb \
  --name aegisvault-j \
  --app-version 0.1.0 \
  --vendor "Aegis Vault" \
  --description "Secure encrypted container for your files" \
  --copyright "Copyright (c) 2026 Aegis Vault" \
  --input build/libs \
  --main-jar aegis-vault-0.1.0-SNAPSHOT.jar \
  --main-class com.aegisvault.AegisVaultApplication \
  --dest dist/linux \
  --java-options "-Xmx512m" \
  --linux-menu-group "Utility;Security" \
  --linux-shortcut \
  --linux-deb-maintainer "dev@aegisvault.example.com" \
  --linux-app-category utils \
  --icon packaging/icons/aegisvault-j.png \
  --module-path "build/libs/dependencies"
```

### Creating an AppImage (Alternative)

For maximum portability, you can create an AppImage after building the app-image:

```bash
# First create an app-image
jpackage --type app-image ... --dest dist/linux

# Then use appimagetool
wget https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage
chmod +x appimagetool-x86_64.AppImage

# Create AppDir structure from jpackage output
./appimagetool-x86_64.AppImage dist/linux/aegisvault-j dist/linux/AegisVault-J-0.1.0-x86_64.AppImage
```

### Adding a Custom Icon

1. Create a 256x256 or 512x512 PNG icon
2. Place at `packaging/icons/aegisvault-j.png`
3. Rebuild

---

## macOS Packaging

### Building the DMG Installer

```bash
# Ensure Xcode CLI tools are installed
xcode-select --install

# Build the .dmg installer
./gradlew packageMacOs
```

### Building App Bundle Only

```bash
./gradlew packageMacOsApp
```

### Output Locations

```
dist/macos/AegisVault-J-0.1.0.dmg
dist/macos/AegisVault-J.app/
```

### What Gets Installed

| Component | Location |
|-----------|----------|
| Application | `/Applications/AegisVault-J.app/` (user choice) |
| Bundled JRE | `AegisVault-J.app/Contents/runtime/` |

### Manual jpackage Command

```bash
# From project root, after ./gradlew build
jpackage \
  --type dmg \
  --name "AegisVault-J" \
  --app-version 0.1.0 \
  --vendor "Aegis Vault" \
  --description "Secure encrypted container for your files" \
  --copyright "Copyright (c) 2026 Aegis Vault" \
  --input build/libs \
  --main-jar aegis-vault-0.1.0-SNAPSHOT.jar \
  --main-class com.aegisvault.AegisVaultApplication \
  --dest dist/macos \
  --java-options "-Xmx512m" \
  --mac-package-name "AegisVault-J" \
  --icon packaging/icons/aegisvault-j.icns \
  --module-path "build/libs/dependencies"
```

### Adding a Custom Icon

1. Create a 1024x1024 PNG icon
2. Convert to `.icns` format
3. Place at `packaging/icons/aegisvault-j.icns`
4. Rebuild

**Icon Conversion:**
```bash
# Create iconset directory
mkdir aegisvault-j.iconset

# Create required sizes
sips -z 16 16   aegisvault-j.png --out aegisvault-j.iconset/icon_16x16.png
sips -z 32 32   aegisvault-j.png --out aegisvault-j.iconset/icon_16x16@2x.png
sips -z 32 32   aegisvault-j.png --out aegisvault-j.iconset/icon_32x32.png
sips -z 64 64   aegisvault-j.png --out aegisvault-j.iconset/icon_32x32@2x.png
sips -z 128 128 aegisvault-j.png --out aegisvault-j.iconset/icon_128x128.png
sips -z 256 256 aegisvault-j.png --out aegisvault-j.iconset/icon_128x128@2x.png
sips -z 256 256 aegisvault-j.png --out aegisvault-j.iconset/icon_256x256.png
sips -z 512 512 aegisvault-j.png --out aegisvault-j.iconset/icon_256x256@2x.png
sips -z 512 512 aegisvault-j.png --out aegisvault-j.iconset/icon_512x512.png
sips -z 1024 1024 aegisvault-j.png --out aegisvault-j.iconset/icon_512x512@2x.png

# Convert to icns
iconutil -c icns aegisvault-j.iconset -o aegisvault-j.icns
```

### Gatekeeper and Code Signing

**Important:** Without code signing, macOS Gatekeeper will block the application. Users must:

1. **First Launch:** Right-click → Open → Click "Open" in dialog
2. **Or:** System Preferences → Security & Privacy → "Open Anyway"

**To sign the application (requires Apple Developer ID):**

```bash
# Sign the app bundle
codesign --deep --force --verify --verbose \
  --sign "Developer ID Application: Your Name (TEAM_ID)" \
  dist/macos/AegisVault-J.app

# Notarize for distribution (macOS 10.15+)
xcrun notarytool submit dist/macos/AegisVault-J-0.1.0.dmg \
  --apple-id "your@email.com" \
  --password "app-specific-password" \
  --team-id "TEAM_ID" \
  --wait

# Staple the notarization
xcrun stapler staple dist/macos/AegisVault-J-0.1.0.dmg
```

---

## Cross-Platform Build Notes

### Building for Other Platforms

jpackage can only create installers for the OS it runs on. For cross-platform builds:

| Target | Build On |
|--------|----------|
| Windows .exe | Windows |
| Linux .deb/.rpm | Linux |
| macOS .dmg/.app | macOS |

### CI/CD Recommendations

Use a CI system with multiple OS runners:

```yaml
# Example GitHub Actions matrix
jobs:
  build:
    strategy:
      matrix:
        os: [windows-latest, ubuntu-latest, macos-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: ./gradlew packageCurrentPlatform
```

---

## Troubleshooting

### Windows

| Issue | Solution |
|-------|----------|
| "WiX Toolset not found" | Install WiX and add to PATH |
| "Access denied" | Run PowerShell as Administrator |
| Icon not showing | Ensure .ico has correct multi-resolution format |

### Linux

| Issue | Solution |
|-------|----------|
| "dpkg-deb: command not found" | `sudo apt install dpkg-dev` |
| "rpmbuild: command not found" | `sudo dnf install rpm-build` |
| Permission errors | Use fakeroot: `fakeroot ./gradlew packageLinuxDeb` |

### macOS

| Issue | Solution |
|-------|----------|
| "Xcode not installed" | `xcode-select --install` |
| App won't open (Gatekeeper) | Right-click → Open, or sign the app |
| Icon not showing | Ensure .icns format is correct |

### General

| Issue | Solution |
|-------|----------|
| "jpackage: command not found" | Ensure JDK 14+ is installed and in PATH |
| Missing dependencies | Run `./gradlew build copyDependencies` first |
| JavaFX errors | Ensure JavaFX modules are in module path |

---

## Release Checklist

Before releasing a new version:

- [ ] Update version in `build.gradle` (`version` and `releaseVersion`)
- [ ] Update version in documentation
- [ ] Run all tests: `./gradlew test`
- [ ] Build for each target platform
- [ ] Test installation on clean VMs
- [ ] Verify application launches correctly
- [ ] Test vault creation and file operations
- [ ] Update release notes

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.1.0 | 2026-01-21 | Initial packaging setup |

---

## Security Considerations

### Bundled Runtime

- The JRE is bundled with the application
- No external Java installation is required or used
- Updates require a new application release

### No Auto-Update

- AegisVault-J does not auto-update
- Users must manually download new versions
- This is intentional for security (no network access)

### No Network Access

- The packaged application makes no network connections
- All operations are local to the user's machine
- Vault files are never transmitted

---

*For security information, see [SECURITY.md](../SECURITY.md)*  
*For project context, see [context.md](context.md)*
