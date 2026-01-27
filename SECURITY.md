# Security Policy — AegisVault-J

## Security Model

AegisVault-J is a **Java-only encrypted container system**. This document describes the security guarantees, limitations, and threat model.

## Cryptographic Specifications

| Component | Specification |
|-----------|---------------|
| Encryption | AES-256-GCM |
| Key Derivation | Argon2id (64 MiB memory, 3 iterations, parallelism 1) |
| IV/Nonce | 96-bit, cryptographically random, unique per encryption |
| Authentication Tag | 128-bit GCM tag |
| Salt | 256-bit, cryptographically random, unique per vault |
| PRNG | `java.security.SecureRandom` |

## Protected Threats

AegisVault-J protects against:

- **Unauthorized file access** — All vault contents are encrypted at rest
- **Brute-force attacks** — Argon2id makes password guessing expensive
- **Data tampering** — GCM authentication detects modifications
- **Weak passwords** — Password strength indicator warns users

## Security Features

### Password Handling
- Passwords stored as `char[]`, never `String`
- Password arrays zeroed immediately after use
- Password strength indicator with entropy calculation

**UI Limitation:** The JavaFX PasswordField component internally stores password text as a Java String before conversion to char[]. This String cannot be zeroed and will persist in memory until garbage collected. This is a platform limitation that cannot be resolved without replacing the JavaFX text input system.

### Auto-Lock
- Configurable inactivity timeout (default: 15 minutes)
- Automatic vault closure after timeout

### File Locking
- Vault file locked during use to prevent concurrent access

## Known Limitations

### JVM Limitations
AegisVault-J runs on the Java Virtual Machine, which introduces inherent limitations:

1. **Memory cannot be guaranteed zeroed** — Garbage collection is unpredictable
2. **No control over swap/page files** — Sensitive data may be written to disk by the OS
3. **JIT compilation** — May create copies of sensitive data
4. **Heap dumps** — Could expose decrypted data if captured while vault is open

### What This Does NOT Provide

| Feature | Status | Reason |
|---------|--------|--------|
| Disk-level encryption | ❌ Not provided | Java application, not a driver |
| Kernel-level security | ❌ Not provided | No native code, no FUSE |
| Forensic-grade deniability | ❌ Not provided | Vault file is identifiable |
| Hidden volumes | ❌ Not provided | Out of scope |
| Memory forensics protection | ❌ Limited | JVM heap is accessible |
| Side-channel resistance | ❌ Limited | No constant-time guarantees |

## Threat Model

### In-Scope Attackers
- Someone who obtains the vault file without the password
- Remote attacker without access to running process
- Casual physical access to stored vault files

### Out-of-Scope Attackers
- Attacker with access to running process memory
- Attacker with ability to install keyloggers
- Nation-state level attackers with hardware access
- Attackers who can manipulate the JVM or OS

## Reporting Vulnerabilities

If you discover a security vulnerability in AegisVault-J:

1. **Do NOT** open a public GitHub issue
2. Contact the maintainers privately
3. Provide details including:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

We aim to respond within 48 hours and will coordinate disclosure.

## Security Checklist for Users

- [ ] Use a strong, unique password (12+ characters, mixed case, numbers, symbols)
- [ ] Keep your vault file in a secure location
- [ ] Don't share your vault password
- [ ] Lock your vault when not in use
- [ ] Keep regular backups of your vault
- [ ] Update to latest version for security fixes

## Audit Status

This project has not been independently audited. Use at your own risk for sensitive data.

---

## ⚠️ Experimental Cryptography Features

AegisVault-J includes **experimental** cryptographic features for research and advanced users. These features are:

- **DISABLED by default**
- **NOT covered by the security audit**
- **NOT recommended for production use**
- **Subject to change or removal**

### Experimental Features (Design Phase)

| Feature | Status | Risk Level |
|---------|--------|------------|
| Pluggable Cipher Framework | Design | Medium |
| Serpent-256-GCM | Design | High |
| Twofish-256-GCM | Design | High |
| Camellia-256-GCM | Design | High |
| Kuznyechik-256-GCM | Design | Very High |
| Cipher Cascades | Design | Very High |
| Alternative KDFs (scrypt, PBKDF2) | Design | High |
| Mouse Entropy Collection | Design | Low |
| Cryptographic Self-Tests | Design | Low |

### SECURITY_AUDIT.md Validity

**CRITICAL:** When ANY experimental feature is enabled:

1. The findings in `SECURITY_AUDIT.md` **DO NOT APPLY**
2. No security guarantees are provided
3. The vault should be considered **EXPERIMENTAL ONLY**
4. Data protection level is **UNKNOWN**

### Enabling Experimental Features

Experimental features require explicit opt-in via system properties:

```
-Daegisvault.experimental.ciphers.enabled=true
-Daegisvault.experimental.cipher.serpent.enabled=true
-Daegisvault.experimental.cascades.enabled=true
```

### Risk Acknowledgment

By enabling experimental features, you acknowledge:

1. You understand these features are NOT security audited
2. You accept full responsibility for any data loss
3. You will NOT use experimental vaults for sensitive data
4. The developers provide NO WARRANTY for experimental features

### Documentation

See [docs/experimental-crypto.md](docs/experimental-crypto.md) for complete documentation of experimental features.

---

*Security is a process, not a feature. This document will be updated as the threat model evolves.*
