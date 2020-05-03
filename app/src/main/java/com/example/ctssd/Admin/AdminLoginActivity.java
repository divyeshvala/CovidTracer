package com.example.ctssd.Admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.ctssd.Activities.RegisterActivity;
import com.example.ctssd.R;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class AdminLoginActivity extends AppCompatActivity
{
    private TextView mLoginFeedbackText;
    private EditText pswd1, adminPd2;
    private Button verify;
    private ProgressBar mLoginProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        mLoginFeedbackText = (TextView) findViewById(R.id.admin_login_form_feedback);
        pswd1 = (EditText) findViewById(R.id.id_pswd);
        adminPd2 = (EditText) findViewById(R.id.id_admin_pswd);
        verify = (Button) findViewById(R.id.id_verifyBTN);
        mLoginProgress = (ProgressBar) findViewById(R.id.admin_login_progress_bar);

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String p1 = pswd1.getText().toString();
                String p2 = adminPd2.getText().toString();

                if(p1.isEmpty() || p2.isEmpty()){
                    mLoginFeedbackText.setText("Please enter both passwords.");
                    mLoginFeedbackText.setVisibility(View.VISIBLE);
                } else {
                    verify.setEnabled(false);
                    mLoginProgress.setVisibility(View.VISIBLE);
                    if(p2.equals("coder22"))
                    {
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("psad");

                        databaseReference.addListenerForSingleValueEvent(new ValueEventListener()
                        {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                            {
                                if(dataSnapshot.exists())
                                {
                                    String ps = dataSnapshot.getValue(String.class);
                                    if(p1.equals(ps))
                                    {
                                        mLoginFeedbackText.setText("Login successfull");
                                        mLoginFeedbackText.setTextColor(Color.BLUE);
                                        startActivity(new Intent(AdminLoginActivity.this, AdminHomeActivity.class));
                                        finish();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError)
                            {
                                mLoginFeedbackText.setText("Login failed. Please try again.");
                                mLoginFeedbackText.setVisibility(View.VISIBLE);
                                verify.setEnabled(true);
                            }
                        });
                    }
                    else
                    {
                        mLoginFeedbackText.setText("password2 is wrong");
                        mLoginFeedbackText.setVisibility(View.VISIBLE);
                        verify.setEnabled(true);
                    }
                }
            }
        });
    }
}
