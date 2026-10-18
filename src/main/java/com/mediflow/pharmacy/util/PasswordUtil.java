package com.mediflow.pharmacy.util;

import org.mindrot.jbcrypt.BCrypt;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for secure password hashing and verification.
 */
public class PasswordUtil {

    public static String hashPassword(String password) {
        if (password == null) return "";
        try {
            return BCrypt.hashpw(password, BCrypt.gensalt(12));
        } catch (Throwable t) {
            return sha256(password);
        }
    }

    public static boolean checkPassword(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null) return false;
        try {
            if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
                return BCrypt.checkpw(candidatePassword, storedHash);
            }
        } catch (Throwable ignored) {
        }
        return storedHash.equals(candidatePassword) || storedHash.equals(sha256(candidatePassword));
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
