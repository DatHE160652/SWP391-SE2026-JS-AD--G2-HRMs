package com.hrms.service;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class BCryptPasswordVerifier {
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || rawPassword.isEmpty() || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        if (!passwordHash.startsWith("$2")) {
            return rawPassword.equals(passwordHash);
        }
        try {
            return BCrypt.verifyer().verify(rawPassword.toCharArray(), passwordHash).verified;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
