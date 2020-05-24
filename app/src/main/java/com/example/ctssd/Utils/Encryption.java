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


    public String encrypt(byte[] plaintext, SecretKey secretKey, byte[] IV )
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
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
}
