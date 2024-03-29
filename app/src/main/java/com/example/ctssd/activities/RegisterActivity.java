package com.example.ctssd.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.Calendar;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private EditText mCountryCode;
    private EditText mPhoneNumber;
    private Button mGenerateBtn;
    private ProgressBar mLoginProgress;
    private TextView mLoginFeedbackText;
    private String complete_phone_number;
    private int counter;

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

        mGenerateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToHome();
//                String country_code = mCountryCode.getText().toString();
//                String phone_number = mPhoneNumber.getText().toString();
//
//                complete_phone_number = country_code + phone_number;
//
//                if(country_code.isEmpty() || phone_number.isEmpty()){
//                    mLoginFeedbackText.setText("Please fill in the form to continue.");
//                    mLoginFeedbackText.setVisibility(View.VISIBLE);
//                } else {
//                    mLoginProgress.setVisibility(View.VISIBLE);
//                    mGenerateBtn.setEnabled(false);
//
//                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
//                            complete_phone_number,
//                            60,
//                            TimeUnit.SECONDS,
//                            RegisterActivity.this,
//                            mCallbacks
//                    );
//                }
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
            sendUserToHome();
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = settings.edit();
                            Calendar c1 = Calendar.getInstance();
                            editor.putInt("startingDay", c1.get(Calendar.DAY_OF_MONTH));
                            editor.putInt("startingMonth", c1.get(Calendar.MONTH));
                            editor.putInt("startingYear", c1.get(Calendar.YEAR));
                            editor.putInt("startingHour", c1.get(Calendar.HOUR_OF_DAY));
                            editor.putInt("startingMinute", c1.get(Calendar.MINUTE));
                            editor.putString("myPhoneNumber", complete_phone_number);
                            editor.apply();

                            addAutoStartup();

                        } else {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                mLoginFeedbackText.setVisibility(View.VISIBLE);
                                mLoginFeedbackText.setText("Some error occured please try again.");
                                mLoginProgress.setVisibility(View.INVISIBLE);
                                mGenerateBtn.setEnabled(true);
                            }
                        }
                    }
                });
    }

    private void getUniqueIdOfUser(final String phone)
    {
        final DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("counter");
        db.addListenerForSingleValueEvent(
                new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {
                            counter = dataSnapshot.getValue(Integer.class);
                            final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Ids").child(phone);
                            dbRef.addValueEventListener(
                                    new ValueEventListener()
                                    {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                        {
                                            if(!dataSnapshot.exists())
                                            {
                                                dbRef.setValue(counter+1);
                                                SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                                                SharedPreferences.Editor edit = settings.edit();
                                                edit.putString("myDeviceId", String.valueOf(counter+1));
                                                edit.apply();
                                                db.setValue(counter+1);
                                                mLoginProgress.setVisibility(View.INVISIBLE);
                                                mGenerateBtn.setEnabled(true);
                                                Log.i("RegisterActivity", "Datasnapshot not exists :"+String.valueOf(counter+1));
                                                sendUserToHome();
                                            }
                                            else
                                            {
                                                SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                                                SharedPreferences.Editor edit = settings.edit();
                                                edit.putString("myDeviceId", String.valueOf(dataSnapshot.getValue(Integer.class)));
                                                edit.apply();
                                                Log.i("Register", "dataSnapshot exists");
                                                mLoginProgress.setVisibility(View.INVISIBLE);
                                                mGenerateBtn.setEnabled(true);
                                                Log.i("RegisterActivity", "Datasnapshot exists :"+String.valueOf(dataSnapshot.getValue(Integer.class)));
                                                sendUserToHome();
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                                    }
                            );
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                }
        );
    }

    private void addAutoStartup() {

        try {
            Intent intent = new Intent();
            String manufacturer = android.os.Build.MANUFACTURER;
            if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if ("Letv".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
            } else if ("Honor".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            }

            List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if  (list.size() > 0) {
                startActivityForResult(intent, 53);
            }
            else
            {
                getUniqueIdOfUser(complete_phone_number);
            }
        } catch (Exception e) {
            Log.e("exc" , String.valueOf(e));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i("RegisterActivity", "Inside on activity result");
        if(requestCode==53)
        {
            Log.i("RegisterActivity", "Returned from autoStart");
            getUniqueIdOfUser(complete_phone_number);
        }
    }

    private void sendUserToHome()
    {
        Intent homeIntent = new Intent(RegisterActivity.this, GetPermissionsActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }
}
