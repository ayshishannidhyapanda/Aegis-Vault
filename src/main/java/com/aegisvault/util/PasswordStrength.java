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
package com.aegisvault.util;

public final class PasswordStrength {

    public enum Strength {
        VERY_WEAK(0, "Very Weak", "#e74c3c"),
        WEAK(1, "Weak", "#e67e22"),
        FAIR(2, "Fair", "#f1c40f"),
        STRONG(3, "Strong", "#27ae60"),
        VERY_STRONG(4, "Very Strong", "#2ecc71");

        private final int score;
        private final String label;
        private final String color;

        Strength(int score, String label, String color) {
            this.score = score;
            this.label = label;
            this.color = color;
        }

        public int getScore() {
            return score;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }
    }

    private PasswordStrength() {
    }

    public static Strength evaluate(String password) {
        if (password == null || password.isEmpty()) {
            return Strength.VERY_WEAK;
        }

        int score = 0;
        int length = password.length();

        if (length >= 8) score++;
        if (length >= 12) score++;
        if (length >= 16) score++;

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        int charTypes = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        score += charTypes;

        if (hasRepeatingPatterns(password)) score--;
        if (hasSequentialChars(password)) score--;

        if (score <= 1) return Strength.VERY_WEAK;
        if (score <= 3) return Strength.WEAK;
        if (score <= 5) return Strength.FAIR;
        if (score <= 6) return Strength.STRONG;
        return Strength.VERY_STRONG;
    }

    public static double calculateEntropy(String password) {
        if (password == null || password.isEmpty()) {
            return 0.0;
        }

        int poolSize = 0;
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        if (hasLower) poolSize += 26;
        if (hasUpper) poolSize += 26;
        if (hasDigit) poolSize += 10;
        if (hasSpecial) poolSize += 32;

        if (poolSize == 0) return 0.0;

        return password.length() * (Math.log(poolSize) / Math.log(2));
    }

    private static boolean hasRepeatingPatterns(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c = password.charAt(i);
            if (c == password.charAt(i + 1) && c == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSequentialChars(String password) {
        String lower = password.toLowerCase();
        for (int i = 0; i < lower.length() - 2; i++) {
            char c1 = lower.charAt(i);
            char c2 = lower.charAt(i + 1);
            char c3 = lower.charAt(i + 2);
            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }
            if (c2 == c1 - 1 && c3 == c2 - 1) {
                return true;
            }
        }
        return false;
    }
}
