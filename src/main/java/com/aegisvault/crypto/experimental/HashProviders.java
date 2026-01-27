/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 */
package com.aegisvault.crypto.experimental;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.digests.WhirlpoolDigest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.Blake2sDigest;
import org.bouncycastle.crypto.digests.GOST3411_2012_256Digest;
import org.bouncycastle.crypto.Digest;

import java.util.Arrays;
import java.util.List;

public final class HashProviders {

    public static final String SHA_512 = "SHA-512";
    public static final String SHA_256 = "SHA-256";
    public static final String WHIRLPOOL = "Whirlpool";
    public static final String BLAKE2S_256 = "BLAKE2s-256";
    public static final String STREEBOG_256 = "Streebog";

    private HashProviders() {
    }

    public static List<String> getAllHashAlgorithms() {
        return Arrays.asList(SHA_512, SHA_256, WHIRLPOOL, BLAKE2S_256, STREEBOG_256);
    }

    public static HashProvider get(String algorithm) {
        return switch (algorithm) {
            case SHA_512 -> new Sha512HashProvider();
            case SHA_256 -> new Sha256HashProvider();
            case WHIRLPOOL -> new WhirlpoolHashProvider();
            case BLAKE2S_256 -> new Blake2sHashProvider();
            case STREEBOG_256 -> new StreebogHashProvider();
            default -> throw new IllegalArgumentException("Unknown hash algorithm: " + algorithm);
        };
    }

    public static class Sha512HashProvider implements HashProvider {
        @Override
        public String getIdentifier() { return SHA_512; }

        @Override
        public int getOutputLengthBytes() { return 64; }

        @Override
        public boolean isExperimental() { return false; }

        @Override
        public byte[] hash(byte[] input) {
            Digest digest = new SHA512Digest();
            byte[] output = new byte[digest.getDigestSize()];
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }

        @Override
        public byte[] hash(byte[] input, byte[] salt) {
            Digest digest = new SHA512Digest();
            byte[] output = new byte[digest.getDigestSize()];
            if (salt != null) {
                digest.update(salt, 0, salt.length);
            }
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }
    }

    public static class Sha256HashProvider implements HashProvider {
        @Override
        public String getIdentifier() { return SHA_256; }

        @Override
        public int getOutputLengthBytes() { return 32; }

        @Override
        public boolean isExperimental() { return false; }

        @Override
        public byte[] hash(byte[] input) {
            Digest digest = new SHA256Digest();
            byte[] output = new byte[digest.getDigestSize()];
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }

        @Override
        public byte[] hash(byte[] input, byte[] salt) {
            Digest digest = new SHA256Digest();
            byte[] output = new byte[digest.getDigestSize()];
            if (salt != null) {
                digest.update(salt, 0, salt.length);
            }
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }
    }

    public static class WhirlpoolHashProvider implements HashProvider {
        @Override
        public String getIdentifier() { return WHIRLPOOL; }

        @Override
        public int getOutputLengthBytes() { return 64; }

        @Override
        public boolean isExperimental() { return true; }

        @Override
        public byte[] hash(byte[] input) {
            Digest digest = new WhirlpoolDigest();
            byte[] output = new byte[digest.getDigestSize()];
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }

        @Override
        public byte[] hash(byte[] input, byte[] salt) {
            Digest digest = new WhirlpoolDigest();
            byte[] output = new byte[digest.getDigestSize()];
            if (salt != null) {
                digest.update(salt, 0, salt.length);
            }
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }
    }

    public static class Blake2sHashProvider implements HashProvider {
        @Override
        public String getIdentifier() { return BLAKE2S_256; }

        @Override
        public int getOutputLengthBytes() { return 32; }

        @Override
        public boolean isExperimental() { return true; }

        @Override
        public byte[] hash(byte[] input) {
            Blake2sDigest digest = new Blake2sDigest(256);
            byte[] output = new byte[digest.getDigestSize()];
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }

        @Override
        public byte[] hash(byte[] input, byte[] salt) {
            Blake2sDigest digest = new Blake2sDigest(256);
            byte[] output = new byte[digest.getDigestSize()];
            if (salt != null) {
                digest.update(salt, 0, salt.length);
            }
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }
    }

    public static class StreebogHashProvider implements HashProvider {
        @Override
        public String getIdentifier() { return STREEBOG_256; }

        @Override
        public int getOutputLengthBytes() { return 32; }

        @Override
        public boolean isExperimental() { return true; }

        @Override
        public byte[] hash(byte[] input) {
            Digest digest = new GOST3411_2012_256Digest();
            byte[] output = new byte[digest.getDigestSize()];
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }

        @Override
        public byte[] hash(byte[] input, byte[] salt) {
            Digest digest = new GOST3411_2012_256Digest();
            byte[] output = new byte[digest.getDigestSize()];
            if (salt != null) {
                digest.update(salt, 0, salt.length);
            }
            digest.update(input, 0, input.length);
            digest.doFinal(output, 0);
            return output;
        }
    }
}
