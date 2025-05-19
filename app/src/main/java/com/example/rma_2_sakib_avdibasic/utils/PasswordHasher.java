package com.example.rma_2_sakib_avdibasic.utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import android.util.Base64;
public class PasswordHasher {
    public static String hashPassword(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = sha256(password, salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    public static boolean verifyPassword(String password, String stored) {
        String[] parts = stored.split(":");
        byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] storedHash = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] computedHash = sha256(password, salt);
        if (storedHash.length != computedHash.length) return false;
        for (int i = 0; i < storedHash.length; i++) {
            if (storedHash[i] != computedHash[i]) return false;
        }
        return true;
    }

    private static byte[] sha256(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(password.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
