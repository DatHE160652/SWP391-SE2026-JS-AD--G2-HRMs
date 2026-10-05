package com.hrms.service;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** Verifies BCrypt hashes without ever accepting plaintext passwords. */
public final class BCryptPasswordVerifier {
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || rawPassword.isEmpty() || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.verifyer().verify(rawPassword.toCharArray(), passwordHash).verified;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
