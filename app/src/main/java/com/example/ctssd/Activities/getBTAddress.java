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
import android.widget.TextView;
import android.widget.Toast;

import com.example.ctssd.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class getBTAddress extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_btaddress);

        Button submit = findViewById(R.id.id_submitMacBTN);
        TextView needHelp = findViewById(R.id.id_needHelp);
        final EditText macEditText = findViewById(R.id.id_macEditText);

        final String phone = getIntent().getStringExtra("myPhoneNumber");

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String myMacAdd = macEditText.getText().toString();
                if(myMacAdd.equals(""))
                {
                    Toast.makeText(getBTAddress.this, "Please enter bluetooth address", Toast.LENGTH_LONG).show();
                }
                else
                {
                    setDetailsAndStatus(myMacAdd, phone);
                }
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
                            SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString("myMacAdd", myMacAdd);
                            editor.apply();

                            Intent homeIntent = new Intent(getBTAddress.this, Main2Activity.class);
                            homeIntent.putExtra("myMacAdd", myMacAdd);
                            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(homeIntent);
                            finish();
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
}
