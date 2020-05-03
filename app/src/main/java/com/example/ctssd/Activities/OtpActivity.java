package com.example.ctssd.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.ctssd.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OtpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;

    private String mAuthVerificationId;

    private EditText mOtpText;
    private Button mVerifyBtn;

    private ProgressBar mOtpProgress;

    private TextView mOtpFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        mAuthVerificationId = getIntent().getStringExtra("AuthCredentials");

        mOtpFeedback = findViewById(R.id.otp_form_feedback);
        mOtpProgress = findViewById(R.id.otp_progress_bar);
        mOtpText = findViewById(R.id.otp_text_view);

        mVerifyBtn = findViewById(R.id.verify_btn);

        mVerifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String otp = mOtpText.getText().toString();

                if(otp.isEmpty()){

                    mOtpFeedback.setVisibility(View.VISIBLE);
                    mOtpFeedback.setText("Please fill in the form and try again.");

                } else {

                    mOtpProgress.setVisibility(View.VISIBLE);
                    mVerifyBtn.setEnabled(false);

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mAuthVerificationId, otp);
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(OtpActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //setDetailsAndStatus(myMacAdd, phoneNum);
                            sendUserToGetMac();
                            // ...
                        } else {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                mOtpFeedback.setVisibility(View.VISIBLE);
                                mOtpFeedback.setText("Some error occured please try again.");
                            }
                        }
                        mOtpProgress.setVisibility(View.INVISIBLE);
                        mVerifyBtn.setEnabled(true);
                    }
                });
    }

    private void setDetailsAndStatus(final String myMacAdd, final String phone)
    {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Users");
        dbRef.child(myMacAdd).child("status").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(!dataSnapshot.exists())
                        {
                            dbRef.child(myMacAdd).child("status").setValue("safe");
                            dbRef.child(myMacAdd).child("phone").setValue(phone);
                        }
                        else
                        {
                            Log.i("Register", "dataSnapshot exists");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                }
        );
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        if(mCurrentUser != null){
//            sendUserToHome();
//        }
//    }

    public void sendUserToGetMac()
    {
        String phone = getIntent().getStringExtra("myPhoneNumber");
        Intent homeIntent = new Intent(OtpActivity.this, getBTAddress.class);
        homeIntent.putExtra("myPhoneNumber", phone);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }
}

