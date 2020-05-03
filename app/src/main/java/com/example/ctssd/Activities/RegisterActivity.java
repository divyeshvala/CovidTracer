package com.example.ctssd.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.ctssd.Admin.AdminHomeActivity;
import com.example.ctssd.Admin.AdminLoginActivity;
import com.example.ctssd.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
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

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private EditText mCountryCode;
    private EditText mPhoneNumber;
    private Button mGenerateBtn, adminLogin;
    private ProgressBar mLoginProgress;
    private TextView mLoginFeedbackText;
    private String complete_phone_number;
    private BluetoothAdapter bluetoothAdapter;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        mCountryCode = findViewById(R.id.country_code_text);
        mPhoneNumber = findViewById(R.id.phone_number_text);
        mGenerateBtn = findViewById(R.id.generate_btn);
        mLoginProgress = findViewById(R.id.login_progress_bar);
        mLoginFeedbackText = findViewById(R.id.login_form_feedback);
        adminLogin = findViewById(R.id.id_adminLogin);

        adminLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, AdminLoginActivity.class));
                finish();
            }
        });

        mGenerateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String country_code = mCountryCode.getText().toString();
                String phone_number = mPhoneNumber.getText().toString();

                complete_phone_number = country_code + phone_number;

                if(country_code.isEmpty() || phone_number.isEmpty()){
                    mLoginFeedbackText.setText("Please fill in the form to continue.");
                    mLoginFeedbackText.setVisibility(View.VISIBLE);
                } else {
                    mLoginProgress.setVisibility(View.VISIBLE);
                    mGenerateBtn.setEnabled(false);

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            complete_phone_number,
                            60,
                            TimeUnit.SECONDS,
                            RegisterActivity.this,
                            mCallbacks
                    );
                }
            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                mLoginFeedbackText.setText("Verification Failed, please try again.");
                mLoginFeedbackText.setVisibility(View.VISIBLE);
                mLoginProgress.setVisibility(View.INVISIBLE);
                mGenerateBtn.setEnabled(true);
            }

            @Override
            public void onCodeSent(final String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);

                new android.os.Handler().postDelayed(
                        new Runnable() {
                            public void run() {
                                Intent otpIntent = new Intent(RegisterActivity.this, OtpActivity.class);
                                otpIntent.putExtra("myPhoneNumber", complete_phone_number);
                                otpIntent.putExtra("AuthCredentials", s);
                                startActivity(otpIntent);
                            }
                        },
                        10000);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mCurrentUser!=null)
        {
            SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
            String myMacAdd = settings.getString("myMacAdd", "");
            sendUserToHome(myMacAdd);
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //setDetailsAndStatus(myMacAdd, complete_phone_number);
                            sendUserToGetMac(complete_phone_number);

                        } else {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                mLoginFeedbackText.setVisibility(View.VISIBLE);
                                mLoginFeedbackText.setText("Some error occured please try again.");
                            }
                        }
                        mLoginProgress.setVisibility(View.INVISIBLE);
                        mGenerateBtn.setEnabled(true);
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

    private void sendUserToHome(String myMacAdd)
    {
        Intent homeIntent = new Intent(RegisterActivity.this, Main2Activity.class);
        homeIntent.putExtra("myMacAdd", myMacAdd);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }

    private void sendUserToGetMac(String phone)
    {
        Intent homeIntent = new Intent(RegisterActivity.this, getBTAddress.class);
        homeIntent.putExtra("myPhoneNumber", phone);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }
}
