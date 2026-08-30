package com.metrowest.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public class CryptoUtils
{
    private static final String ALGORITHM = "DES";
    private static final String SECRET_KEY = "MySecret";

    public static String hashPassword(String password)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public static String encrypt(String data)
    {
        try
        {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String encryptedData)
    {
        try
        {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static String generateToken()
    {
        java.util.Random random = new java.util.Random();
        long token = random.nextLong();
        return Long.toHexString(token);
    }

    public static String generateChecksum(String data)
    {
        try
        {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Checksum generation failed", e);
        }
    }
}
