package com.example.ctssd.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.ctssd.R;
import com.example.ctssd.Utils.Decryption;
import com.example.ctssd.Utils.Encryption;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class TempActivity extends AppCompatActivity {

    KeyGenerator keyGenerator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp);

        final String strSecretKey;
        //final byte[] IV = new byte[16];
        //final SecretKey secretKey;
        //byte[] secretKeyen;
        //SecureRandom random;

        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //keyGenerator.init(256);
        //secretKey = keyGenerator.generateKey();
        //secretKeyen=secretKey.getEncoded();
//        String temp = "";
//        int i=1;
//        for(byte b : secretKeyen)
//        {
//            temp += b+", ";
//        }
//        Log.i("TempAct", temp);


        byte[] publicKey = {43, -84, 15, 80, 43, -6, -1, 13, -81, 62, -19, -32, -82, 112, 24, -79, -77, 83, -100, 101, -8, 100, 51, 18, -63, -114, 46, -11, -113, -12, 63, 76};
        //strSecretKey = encoderfun(secretKeyen);   // will be used for decryption.
        strSecretKey = encoderfun(publicKey);
        //random = new SecureRandom();
        //random.nextBytes(IV);
        //final String strIV = encoderfun(IV);   // will be used for decryption

        final EditText encText = findViewById(R.id.encrypted);
        final TextView decText = findViewById(R.id.decrypted);

        findViewById(R.id.doItBTN).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Encryption encObject = new Encryption();
                Decryption decObject = new Decryption();
                String text = encObject.encrypt(encText.getText().toString().trim().getBytes(), strSecretKey);
                Log.i("TempAct", "Name - "+ BluetoothAdapter.getDefaultAdapter().getName());
                Log.i("TempAct", text);
                BluetoothAdapter.getDefaultAdapter().setName(text);
                String dec = decObject.decryption(text, strSecretKey);

                decText.setText(dec);

            }
        });

    }

    private static String encoderfun(byte[] decval) {
        return Base64.encodeToString(decval,Base64.DEFAULT);
    }
}
