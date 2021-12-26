package com.example.ctssd.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ctssd.R;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    sleep(2500);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    Intent intent = new Intent(SplashActivity.this, RegisterActivity.class);
                    startActivity(intent);

                    finish();
                }
            }
        };
        thread.start();
    }
}
