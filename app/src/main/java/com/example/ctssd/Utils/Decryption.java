package com.example.ctssd.Utils;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Decryption
{
    public String decryption(String text, String secretKey, String ivKey)
    {
        try {
            String strEncText = text.trim();
            byte[] encText = decoderfun(strEncText);
            String strAnahtar = ivKey.trim();
            byte[] iv = decoderfun(strAnahtar);
            String strSecretKey = secretKey.trim();
            byte[] encodedSecretKey = decoderfun(strSecretKey);
            SecretKey originalSecretKey = new SecretKeySpec(encodedSecretKey, 0, encodedSecretKey.length, "AES");

            try {
                Cipher cipher = Cipher.getInstance("AES");
                SecretKeySpec keySpec = new SecretKeySpec(originalSecretKey.getEncoded(), "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
                //cipher.init(Cipher.DECRYPT_MODE, keySpec);
                byte[] decryptedText = cipher.doFinal(encText);
                return new String(decryptedText);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }

    private static byte[] decoderfun(String enval) {
        return Base64.decode(enval,Base64.DEFAULT);
    }
}
