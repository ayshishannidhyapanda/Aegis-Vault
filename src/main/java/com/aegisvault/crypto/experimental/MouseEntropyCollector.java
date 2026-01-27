/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 *
 * This software, known as "AegisVault-J", including its source code, documentation,
 * design, and associated materials, is the intellectual property of the author.
 *
 * No part of this software may be copied, modified, distributed, or used in
 * derivative works without explicit written permission from the copyright holder,
 * except for academic evaluation purposes.
 *
 * This software is provided "as is", without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose, and noninfringement.
 */
package com.aegisvault.crypto.experimental;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class MouseEntropyCollector {

    private static final String LIMITATION_WARNING =
            "Mouse entropy collection is SUPPLEMENTARY only. " +
            "It does NOT replace SecureRandom system entropy. " +
            "Entropy quality from mouse movements is NOT guaranteed. " +
            "This feature is EXPERIMENTAL and provides no proven security benefit.";

    private final AtomicLong entropyPool = new AtomicLong(0);
    private final AtomicLong sampleCount = new AtomicLong(0);
    private volatile boolean collecting = false;
    private volatile long lastTimestamp = 0;

    public MouseEntropyCollector() {
        System.err.println("[EXPERIMENTAL ENTROPY] " + LIMITATION_WARNING);
    }

    public void startCollecting() {
        collecting = true;
        lastTimestamp = System.nanoTime();
        sampleCount.set(0);
        entropyPool.set(System.nanoTime() ^ Runtime.getRuntime().freeMemory());
    }

    public void stopCollecting() {
        collecting = false;
    }

    public boolean isCollecting() {
        return collecting;
    }

    public void onMouseMoved(double x, double y) {
        if (!collecting) {
            return;
        }

        long currentTime = System.nanoTime();
        long timeDelta = currentTime - lastTimestamp;
        lastTimestamp = currentTime;

        long contribution = Double.doubleToLongBits(x) ^
                           Long.rotateLeft(Double.doubleToLongBits(y), 17) ^
                           Long.rotateLeft(timeDelta, 31) ^
                           Long.rotateLeft(currentTime, 47);

        entropyPool.updateAndGet(v -> v ^ contribution);
        sampleCount.incrementAndGet();
    }

    public void onMouseClicked(double x, double y, int button) {
        if (!collecting) {
            return;
        }

        long currentTime = System.nanoTime();
        long contribution = Double.doubleToLongBits(x) ^
                           Long.rotateLeft(Double.doubleToLongBits(y), 23) ^
                           Long.rotateLeft(currentTime, 41) ^
                           (button * 0x9E3779B97F4A7C15L);

        entropyPool.updateAndGet(v -> v ^ contribution);
        sampleCount.incrementAndGet();
    }

    public long getSampleCount() {
        return sampleCount.get();
    }

    public void contributeToSecureRandom(SecureRandom secureRandom) {
        if (secureRandom == null) {
            throw new IllegalArgumentException("SecureRandom must not be null");
        }

        long entropy = entropyPool.getAndSet(System.nanoTime());
        byte[] entropyBytes = ByteBuffer.allocate(8).putLong(entropy).array();

        secureRandom.nextBytes(new byte[1]);

        secureRandom.setSeed(entropyBytes);

        System.err.println("[EXPERIMENTAL ENTROPY] Contributed " + sampleCount.get() +
                " samples to SecureRandom (supplementary only)");
    }

    public void reset() {
        entropyPool.set(0);
        sampleCount.set(0);
        collecting = false;
    }
}
