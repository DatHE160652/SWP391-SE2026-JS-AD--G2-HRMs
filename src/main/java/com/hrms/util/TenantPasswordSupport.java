package com.hrms.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Versioned tenant credentials; only unmarked legacy values are plaintext. */
public final class TenantPasswordSupport {
    private static final String PREFIX = "$hrm$pbkdf2-sha256$v1$";
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private TenantPasswordSupport() { }
    public static String temporaryPassword() {
        byte[] bytes = new byte[24]; RANDOM.nextBytes(bytes);
        try { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
        finally { Arrays.fill(bytes, (byte) 0); }
    }
    public static String hash(String password) {
        if (password == null || password.isEmpty() || password.length() > 1024) throw new IllegalArgumentException("Mật khẩu phải có từ 1 đến 1024 ký tự.");
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        byte[] key = derive(password, salt);
        try { return PREFIX + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                    + "$" + Base64.getEncoder().encodeToString(key); }
        finally { Arrays.fill(key, (byte) 0); Arrays.fill(salt, (byte) 0); }
    }
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null || password.isEmpty() || password.length() > 1024) return false;
        if (!stored.startsWith("$hrm$")) return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
        if (!stored.startsWith(PREFIX) || stored.length() > 255) return false;
        String[] parts = stored.substring(PREFIX.length()).split("\\$", -1);
        if (parts.length != 3 || !parts[0].equals(Integer.toString(ITERATIONS))
                || parts[1].length() != 24 || parts[2].length() != 44) return false;
        byte[] salt = null, expected = null, actual = null;
        try {
            salt = Base64.getDecoder().decode(parts[1]); expected = Base64.getDecoder().decode(parts[2]);
            if (salt.length != 16 || expected.length != 32) return false;
            actual = derive(password, salt);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) { return false; }
        finally {
            if (salt != null) Arrays.fill(salt, (byte) 0);
            if (expected != null) Arrays.fill(expected, (byte) 0);
            if (actual != null) Arrays.fill(actual, (byte) 0);
        }
    }
    private static byte[] derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (java.security.GeneralSecurityException exception) { throw new IllegalStateException("Password hashing unavailable.", exception); }
        finally { spec.clearPassword(); }
    }
}
