package com.hrms.service;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** Creates BCrypt hashes for generated and changed passwords. */
public final class BCryptPasswordHasher {
    private static final int COST = 12;

    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        }
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }
}
