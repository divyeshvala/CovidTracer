package com.example.ctssd.Utils;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Encryption
{


    public String encrypt(byte[] plaintext, String secretKey)
    {
        String strSecretKey = secretKey.trim();
        byte[] encodedSecretKey = decoderfun(strSecretKey);
        SecretKey originalSecretKey = new SecretKeySpec(encodedSecretKey, 0, encodedSecretKey.length, "AES");
        try
        {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(originalSecretKey.getEncoded(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] cipherText = cipher.doFinal(plaintext);

            return encoderfun(cipherText);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }

    private static String encoderfun(byte[] decval) {
        return Base64.encodeToString(decval,Base64.DEFAULT);
    }

    private static byte[] decoderfun(String enval) {
        return Base64.decode(enval,Base64.DEFAULT);
    }
}
