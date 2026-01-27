# AegisVault-J — Experimental Cryptography

> **Document Status:** DESIGN PHASE  
> **Created:** January 21, 2026  
> **Classification:** EXPERIMENTAL — NOT FOR PRODUCTION USE

---

## ⚠️ CRITICAL WARNING ⚠️

**This document describes EXPERIMENTAL cryptographic features that:**

1. Have NOT been security audited
2. May introduce vulnerabilities
3. Are NOT enabled by default
4. May be removed without notice
5. VOID the SECURITY_AUDIT.md findings when enabled

**The audited default configuration (AES-256-GCM + Argon2id) remains the ONLY recommended option for production use.**

---

## 1. Overview

This document outlines experimental cryptographic extensions for AegisVault-J. These features are designed for:

- Security researchers testing alternative algorithms
- Advanced users with specific compliance requirements
- Educational purposes and algorithm comparison
- Cryptographic experimentation in controlled environments

### 1.1 Non-Negotiable Principles

| Principle | Enforcement |
|-----------|-------------|
| AES-256-GCM is DEFAULT | Cannot be disabled, always available |
| Experimental = Opt-in | User must explicitly enable each experimental feature |
| No false security claims | Documentation is honest about risks |
| Audited path unchanged | Default code path is frozen |
| Clear warnings | UI and logs indicate experimental mode |

### 1.2 Feature Status

| Feature | Status | Risk Level |
|---------|--------|------------|
| Pluggable Cipher Framework | Design Phase | Medium |
| Alternative Ciphers (Serpent, Twofish, etc.) | Design Phase | High |
| Cipher Cascades | Design Phase | Very High |
| Alternative KDFs | Design Phase | High |
| Mouse Entropy Collection | Design Phase | Low |
| Cryptographic Test Vectors | Design Phase | Low |

---

## 2. Pluggable Cipher Framework

### 2.1 Design Goals

Create an abstraction layer that:
- Allows algorithm substitution without changing vault format
- Maintains the existing AES-256-GCM implementation unchanged
- Provides a clean interface for experimental ciphers
- Enables algorithm identification in vault headers

### 2.2 Interface Design

```java
public interface CipherProvider {
    
    String getAlgorithmIdentifier();
    
    int getKeyLengthBytes();
    
    int getIvLengthBytes();
    
    int getTagLengthBytes();
    
    boolean isExperimental();
    
    byte[] encrypt(byte[] plaintext, byte[] key, byte[] aad);
    
    byte[] decrypt(byte[] ciphertext, byte[] key, byte[] aad);
    
    default void validateKey(byte[] key) {
        if (key == null || key.length != getKeyLengthBytes()) {
            throw new IllegalArgumentException(
                "Key must be " + getKeyLengthBytes() + " bytes");
        }
    }
}
```

### 2.3 Implementation Hierarchy

```
CipherProvider (interface)
├── AesGcmCipherProvider (DEFAULT, AUDITED)
│   └── Wraps existing AesGcmCipher
│
└── ExperimentalCipherProvider (abstract)
    ├── SerpentGcmCipherProvider (EXPERIMENTAL)
    ├── TwofishGcmCipherProvider (EXPERIMENTAL)
    ├── CamelliaGcmCipherProvider (EXPERIMENTAL)
    └── KuznyechikGcmCipherProvider (EXPERIMENTAL)
```

### 2.4 Algorithm Registry

```java
public final class CipherRegistry {
    
    private static final Map<String, Supplier<CipherProvider>> PROVIDERS;
    
    static {
        PROVIDERS = new LinkedHashMap<>();
        PROVIDERS.put("AES-256-GCM", AesGcmCipherProvider::new);
    }
    
    public static void registerExperimental(
            String id, 
            Supplier<CipherProvider> provider) {
        // Requires explicit enable flag
        // Logs warning when called
    }
    
    public static CipherProvider getDefault() {
        return new AesGcmCipherProvider();
    }
    
    public static CipherProvider get(String algorithmId) {
        // Returns provider or throws if unknown/disabled
    }
}
```

### 2.5 Vault Header Extension

The vault header format must store the cipher algorithm identifier:

```
[Extended Header]
  - Cipher ID length: uint8 (max 32)
  - Cipher ID: UTF-8 string (e.g., "AES-256-GCM")
  - Flags: uint16
    - Bit 0: Experimental mode enabled
    - Bit 1: Cascade mode enabled
    - Bits 2-15: Reserved
```

### 2.6 Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Weaker algorithm selected | Medium | High | Clear warnings, documentation |
| Implementation bugs | High | Critical | No production use, limited testing |
| Interoperability issues | Medium | Medium | Algorithm ID in header |
| Key management errors | Medium | High | Shared validation logic |

---

## 3. Alternative Cipher Implementations

### 3.1 Serpent-256-GCM

**Status:** EXPERIMENTAL  
**Provider:** BouncyCastle  
**Block Size:** 128 bits  
**Key Size:** 256 bits

**Rationale:** Serpent was a finalist in the AES competition, known for its conservative design with 32 rounds (vs. AES's 14 for 256-bit keys).

**Risks:**
- Less hardware optimization than AES
- Slower performance
- Less cryptanalysis than AES
- GCM mode with Serpent is less studied

**Implementation Notes:**
```java
// BouncyCastle provides SerpentEngine
// Must wrap in GCM mode manually
// IV handling: same as AES-GCM (96-bit recommended)
```

### 3.2 Twofish-256-GCM

**Status:** EXPERIMENTAL  
**Provider:** BouncyCastle  
**Block Size:** 128 bits  
**Key Size:** 256 bits

**Rationale:** Another AES finalist by Bruce Schneier's team, with key-dependent S-boxes.

**Risks:**
- Complex key schedule
- Less studied in GCM mode
- No hardware acceleration

### 3.3 Camellia-256-GCM

**Status:** EXPERIMENTAL  
**Provider:** BouncyCastle  
**Block Size:** 128 bits  
**Key Size:** 256 bits

**Rationale:** Japanese cipher approved by ISO/IEC and used in TLS.

**Risks:**
- Less common outside Japan/EU
- Performance characteristics vary
- Limited GCM mode testing

### 3.4 Kuznyechik-256-GCM (GOST R 34.12-2015)

**Status:** EXPERIMENTAL  
**Provider:** BouncyCastle (if available)  
**Block Size:** 128 bits  
**Key Size:** 256 bits

**Rationale:** Russian GOST standard cipher.

**Risks:**
- Concerns about design transparency
- Limited independent cryptanalysis
- Non-standard GCM mode application
- May not be available in all BouncyCastle versions

**Special Warning:**
> This cipher is included for completeness and research purposes only.
> Its design process was not public, and some cryptographers have raised
> concerns about potential weaknesses. DO NOT use for sensitive data.

---

## 4. Cipher Cascades

### 4.1 Concept

Cipher cascading encrypts data multiple times with different algorithms and keys. The theory is that if one cipher is broken, the others still protect the data.

### 4.2 ⚠️ IMPORTANT SECURITY NOTICE ⚠️

**Cascading does NOT necessarily increase security and may DECREASE it:**

| Concern | Explanation |
|---------|-------------|
| Complexity ≠ Security | More code = more bugs |
| Key management | Multiple keys increase attack surface |
| Performance | Significantly slower |
| Authentication | GCM tags at each layer add complexity |
| Interoperability | Non-standard construction |
| Analysis | Combined behavior is less studied |

**Academic Opinion:** Most cryptographers recommend using a single well-analyzed algorithm (AES-GCM) rather than cascading multiple ciphers.

### 4.3 Predefined Cascades Only

To limit risk, only the following fixed cascades are available:

| Cascade ID | Algorithms | Key Length | Notes |
|------------|------------|------------|-------|
| `CASCADE-AES-SERPENT` | AES-256 → Serpent-256 | 512 bits | Two layers |
| `CASCADE-SERPENT-TWOFISH` | Serpent-256 → Twofish-256 | 512 bits | Two layers |
| `CASCADE-AES-TWOFISH-SERPENT` | AES-256 → Twofish-256 → Serpent-256 | 768 bits | Three layers |

**User-defined cascades are NOT allowed.**

### 4.4 Cascade Implementation

```java
public final class CascadeCipherProvider implements CipherProvider {
    
    private final List<CipherProvider> layers;
    private final String cascadeId;
    
    // Keys are derived independently for each layer
    // KDF output is expanded to provide sufficient key material
    
    @Override
    public byte[] encrypt(byte[] plaintext, byte[] masterKey, byte[] aad) {
        byte[] data = plaintext;
        for (CipherProvider layer : layers) {
            byte[] layerKey = deriveLayerKey(masterKey, layer.getAlgorithmIdentifier());
            data = layer.encrypt(data, layerKey, aad);
            zeroBytes(layerKey);
        }
        return data;
    }
    
    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] masterKey, byte[] aad) {
        byte[] data = ciphertext;
        // Decrypt in reverse order
        for (int i = layers.size() - 1; i >= 0; i--) {
            CipherProvider layer = layers.get(i);
            byte[] layerKey = deriveLayerKey(masterKey, layer.getAlgorithmIdentifier());
            data = layer.decrypt(data, layerKey, aad);
            zeroBytes(layerKey);
        }
        return data;
    }
}
```

### 4.5 Key Derivation for Cascades

Each layer requires an independent key. The master key is expanded using HKDF:

```
Master Key (from Argon2id)
    │
    ├─→ HKDF-Expand(info="AES-256-GCM:layer1") → Layer 1 Key
    ├─→ HKDF-Expand(info="Serpent-256-GCM:layer2") → Layer 2 Key
    └─→ HKDF-Expand(info="Twofish-256-GCM:layer3") → Layer 3 Key
```

---

## 5. Alternative Hash / KDF Options

### 5.1 Overview

The default KDF is Argon2id. These alternatives are for experimentation only.

### 5.2 Available Alternatives

| Algorithm | Type | Status | Use Case |
|-----------|------|--------|----------|
| Argon2id | KDF | DEFAULT, AUDITED | Password hashing |
| scrypt | KDF | EXPERIMENTAL | Memory-hard alternative |
| PBKDF2-SHA512 | KDF | EXPERIMENTAL | Legacy compatibility |
| HKDF-SHA512 | KDF | EXPERIMENTAL | Key expansion only |

### 5.3 Hash Functions for KDF Experiments

| Hash | Output Size | Status | Notes |
|------|-------------|--------|-------|
| SHA-512 | 512 bits | EXPERIMENTAL | Standard, widely analyzed |
| BLAKE2b | Variable | EXPERIMENTAL | Fast, modern |
| BLAKE2s | Variable | EXPERIMENTAL | Optimized for 32-bit |
| Whirlpool | 512 bits | EXPERIMENTAL | AES-based design |
| Streebog | 256/512 bits | EXPERIMENTAL | Russian GOST hash |

### 5.4 ⚠️ Warning About Alternative KDFs ⚠️

**Argon2id is specifically designed for password hashing:**
- Memory-hard (resists GPU attacks)
- Time-hard (resists parallel attacks)
- Winner of Password Hashing Competition

**Alternatives like PBKDF2 are WEAKER:**
- Not memory-hard
- Vulnerable to GPU/ASIC attacks
- Only included for legacy testing

### 5.5 Interface Design

```java
public interface KeyDerivationFunction {
    
    String getIdentifier();
    
    boolean isExperimental();
    
    byte[] deriveKey(char[] password, byte[] salt, int outputLength);
    
    int getRecommendedSaltLength();
    
    Map<String, Object> getParameters();
}
```

---

## 6. Mouse Entropy Collection

### 6.1 Purpose

Supplement (NOT replace) the system's SecureRandom with additional entropy from mouse movements.

### 6.2 ⚠️ Critical Limitations ⚠️

| Limitation | Explanation |
|------------|-------------|
| NOT a replacement | SecureRandom is always primary |
| Quality unknown | Mouse entropy quality is not guaranteed |
| Not available on headless | No mouse in server environments |
| Timing attacks | Collection timing may be observable |
| User behavior | Predictable mouse patterns reduce entropy |

### 6.3 Design Principles

1. **Supplementary only:** Entropy is XORed into existing SecureRandom state
2. **Non-blocking:** Collection never delays cryptographic operations
3. **Optional:** Disabled by default
4. **Transparent:** User knows when collection is active
5. **No claims:** No security improvements are claimed

### 6.4 Implementation Approach

```java
public final class MouseEntropyCollector {
    
    private final AtomicLong entropyPool = new AtomicLong();
    private volatile boolean collecting = false;
    
    public void onMouseMoved(double x, double y, long timestamp) {
        if (!collecting) return;
        
        // Mix position and timing into pool
        long contribution = Double.doubleToLongBits(x) ^ 
                           Double.doubleToLongBits(y) ^ 
                           timestamp ^ 
                           System.nanoTime();
        entropyPool.updateAndGet(v -> v ^ contribution);
    }
    
    public void contributeToSecureRandom(SecureRandom random) {
        // Add collected entropy as supplementary seed
        // Does NOT replace system entropy
        long entropy = entropyPool.getAndSet(0);
        byte[] bytes = ByteBuffer.allocate(8).putLong(entropy).array();
        random.nextBytes(new byte[1]); // Ensure generator is initialized
        // Mix into state via setSeed (additive, per SecureRandom contract)
        random.setSeed(bytes);
    }
}
```

### 6.5 UI Integration

```
┌─────────────────────────────────────────────────────┐
│  [!] Mouse Entropy Collection Active                │
│                                                     │
│  Move your mouse randomly to contribute entropy.    │
│  This supplements (does not replace) system RNG.    │
│                                                     │
│  Collected: ████████░░░░░░░░ (52%)                  │
│                                                     │
│  [Cancel]                          [Done]           │
└─────────────────────────────────────────────────────┘
```

---

## 7. Cryptographic Test Vectors Mode

### 7.1 Purpose

Provide deterministic, reproducible cryptographic operations for:
- Known-answer tests (KAT)
- NIST test vector validation
- Regression testing
- Debugging

### 7.2 ⚠️ NEVER USE IN PRODUCTION ⚠️

Test vectors mode uses **predictable values** for:
- IVs/Nonces
- Salts
- Random generation

**This completely destroys security. It exists only for testing.**

### 7.3 Activation

Test vectors mode requires:
1. Compile-time flag (`-DTEST_VECTORS_ENABLED=true`)
2. Runtime property (`-Daegisvault.testmode=true`)
3. Environment variable (`AEGISVAULT_TEST_MODE=1`)
4. All three must be set simultaneously

### 7.4 Test Vector Provider

```java
@TestOnly
public final class TestVectorProvider {
    
    private static final byte[] DETERMINISTIC_KEY = 
        hexToBytes("000102030405060708090A0B0C0D0E0F" +
                   "101112131415161718191A1B1C1D1E1F");
    
    private static final byte[] DETERMINISTIC_IV =
        hexToBytes("000102030405060708090A0B");
    
    private static final byte[] DETERMINISTIC_SALT =
        hexToBytes("000102030405060708090A0B0C0D0E0F" +
                   "101112131415161718191A1B1C1D1E1F");
    
    // Provides deterministic values for testing only
    // Throws IllegalStateException if test mode not enabled
}
```

### 7.5 Self-Test on Startup

Optional self-test validates cryptographic implementation:

```java
public final class CryptoSelfTest {
    
    public static void runOnStartup() {
        verifyAesGcmEncryption();
        verifyAesGcmDecryption();
        verifyArgon2idDerivation();
        verifySecureRandomEntropy();
    }
    
    private static void verifyAesGcmEncryption() {
        // Known input → Known output comparison
        // Throws CryptoException if mismatch
    }
}
```

---

## 8. Configuration

### 8.1 Experimental Features Configuration

```properties
# Experimental Cryptography Configuration
# ALL OPTIONS BELOW ARE DISABLED BY DEFAULT

# Enable experimental cipher framework
aegisvault.experimental.ciphers.enabled=false

# Enable specific experimental ciphers
aegisvault.experimental.cipher.serpent.enabled=false
aegisvault.experimental.cipher.twofish.enabled=false
aegisvault.experimental.cipher.camellia.enabled=false
aegisvault.experimental.cipher.kuznyechik.enabled=false

# Enable cascade mode
aegisvault.experimental.cascades.enabled=false

# Enable alternative KDFs
aegisvault.experimental.kdf.alternatives.enabled=false

# Enable mouse entropy collection
aegisvault.experimental.entropy.mouse.enabled=false

# Enable startup self-test
aegisvault.crypto.selftest.enabled=false
```

### 8.2 UI Warning When Experimental Mode Active

When any experimental feature is enabled:

```
┌──────────────────────────────────────────────────────────────────┐
│  ⚠️  EXPERIMENTAL CRYPTOGRAPHY MODE ACTIVE                       │
│                                                                  │
│  This vault uses non-standard cryptographic options that:        │
│  • Have NOT been security audited                                │
│  • May contain implementation bugs                               │
│  • Are NOT recommended for production use                        │
│                                                                  │
│  The security audit (SECURITY_AUDIT.md) does NOT apply to        │
│  vaults created with experimental options.                       │
│                                                                  │
│  Active Experimental Features:                                   │
│  • Cipher: Serpent-256-GCM                                       │
│  • Cascade: None                                                 │
│  • KDF: Argon2id (default)                                       │
│                                                                  │
│  [I understand the risks]                    [Cancel]            │
└──────────────────────────────────────────────────────────────────┘
```

---

## 9. File Format Compatibility

### 9.1 Vault Version

Vaults with experimental features use a distinct format version:

| Version | Meaning |
|---------|---------|
| 1 | Standard vault (AES-256-GCM, Argon2id) |
| 100+ | Experimental vault (check extended header) |

### 9.2 Extended Header Format

```
[Standard Header - 64 bytes]
  - Magic: "AEGISVLT" (8 bytes)
  - Version: 100 (uint16) - indicates experimental
  - Flags: (uint16)
  - Salt: (32 bytes)
  - IV: (12 bytes)
  - Reserved: (10 bytes)

[Extended Header - Variable]
  - Extended header length: uint32
  - Cipher ID length: uint8
  - Cipher ID: UTF-8 string
  - KDF ID length: uint8
  - KDF ID: UTF-8 string
  - Cascade layer count: uint8
  - For each cascade layer:
    - Layer cipher ID length: uint8
    - Layer cipher ID: UTF-8 string
  - Additional parameters: (variable)
  - Checksum: uint32 (CRC32 of extended header)
```

### 9.3 Backward Compatibility

| Scenario | Behavior |
|----------|----------|
| Standard vault opened | Works normally |
| Experimental vault opened without experimental mode | Error with clear message |
| Experimental vault opened with wrong cipher available | Error listing required cipher |

---

## 10. Risk Summary

### 10.1 Overall Risk Assessment

| Component | Risk Level | Rationale |
|-----------|------------|-----------|
| Default path (AES-256-GCM) | Low | Audited, unchanged |
| Alternative ciphers | High | Less analysis, implementation risk |
| Cascades | Very High | Complex, non-standard, may reduce security |
| Alternative KDFs | High | May weaken password protection |
| Mouse entropy | Low | Supplementary only, cannot harm |
| Test vectors | Critical (test only) | Destroys security if misused |

### 10.2 Recommendations

1. **Most users:** Use defaults only
2. **Security researchers:** Enable specific features for testing
3. **Compliance requirements:** Consult with cryptographers
4. **Education:** Safe for learning about algorithms

---

## 11. Future Considerations

### 11.1 Potential Additions (NOT PLANNED)

| Feature | Status | Notes |
|---------|--------|-------|
| ChaCha20-Poly1305 | Considered | Strong alternative to AES-GCM |
| XChaCha20-Poly1305 | Considered | Extended nonce variant |
| AES-256-GCM-SIV | Considered | Nonce-misuse resistant |
| Post-quantum algorithms | Not yet | Standards not finalized |

### 11.2 Deprecation Policy

Experimental features may be:
- Modified without notice
- Removed in future versions
- Never promoted to production status

---

## 12. References

- NIST SP 800-38D (GCM Mode)
- NIST SP 800-132 (Password-Based Key Derivation)
- RFC 9106 (Argon2)
- RFC 5869 (HKDF)
- AES Competition Final Report
- Password Hashing Competition

---

*This document describes experimental features. Do not rely on them for security.*
