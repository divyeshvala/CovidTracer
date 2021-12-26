package com.example.ctssd.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

import java.util.Calendar;
import java.util.List;

public class OtpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private String mAuthVerificationId;
    private EditText mOtpText;
    private Button mVerifyBtn;
    private ProgressBar mOtpProgress;
    private TextView mOtpFeedback;
    private int counter;

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
            public void onClick(View v)
            {
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

                            SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = settings.edit();
                            Calendar c1 = Calendar.getInstance();
                            editor.putInt("startingDay", c1.get(Calendar.DAY_OF_MONTH));
                            editor.putInt("startingMonth", c1.get(Calendar.MONTH));
                            editor.putInt("startingYear", c1.get(Calendar.YEAR));
                            editor.putInt("startingHour", c1.get(Calendar.HOUR_OF_DAY));
                            editor.putInt("startingMinute", c1.get(Calendar.MINUTE));
                            editor.putString("myPhoneNumber", getIntent().getStringExtra("myPhoneNumber"));
                            editor.apply();

                            addAutoStartup();
                        } else {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                mOtpFeedback.setVisibility(View.VISIBLE);
                                mOtpFeedback.setText("Some error occured please try again.");
                                mOtpProgress.setVisibility(View.INVISIBLE);
                                mVerifyBtn.setEnabled(true);
                            }
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mCurrentUser != null){
            sendUserToHome();
        }
    }

    private void getUniqueIdOfUser(final String phone)
    {
        final DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("counter");
        db.addListenerForSingleValueEvent(
                new ValueEventListener() {
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
                                                mOtpProgress.setVisibility(View.INVISIBLE);
                                                mVerifyBtn.setEnabled(true);
                                                sendUserToHome();
                                            }
                                            else
                                            {
                                                SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                                                SharedPreferences.Editor edit = settings.edit();
                                                edit.putString("myDeviceId", String.valueOf(dataSnapshot.getValue(Integer.class)));
                                                edit.apply();
                                                Log.i("Register", "dataSnapshot exists");
                                                mOtpProgress.setVisibility(View.INVISIBLE);
                                                mVerifyBtn.setEnabled(true);
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
                getUniqueIdOfUser(getIntent().getStringExtra("myPhoneNumber"));
            }
        } catch (Exception e) {
            Log.e("exc" , String.valueOf(e));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i("OTP", "Inside on activity result");
        if(requestCode==53)
        {
            Log.i("OTP", "Returned from autoStart");
            getUniqueIdOfUser(getIntent().getStringExtra("myPhoneNumber"));
        }
    }

    private void sendUserToHome()
    {
        Intent homeIntent = new Intent(OtpActivity.this, GetPermissionsActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }
}

