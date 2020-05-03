package com.example.ctssd.Admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.example.ctssd.R;
import com.example.ctssd.Utils.UserTreeObject;
import com.example.ctssd.Utils.Utilities;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Queue;

public class AdminHomeActivity extends AppCompatActivity {

    private EditText addPhone, searchPhone;
    private Button addPhoneBTN, searchPhoneBTN;
    private ProgressBar progressBar;
    DatabaseReference databaseReference ;
    int yd=-1, md=-1, dd=-1;
    private Queue<UserTreeObject> q;
    private HashMap<String, Integer> visitedMap;
    HashMap<String, Integer> map = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        addPhone = (EditText) findViewById(R.id.id_addNew_phone);
        searchPhone = (EditText) findViewById(R.id.id_search_phone);
        addPhoneBTN = (Button) findViewById(R.id.id_addNew_btn);
        searchPhoneBTN = (Button) findViewById(R.id.id_search_btn);
        progressBar = findViewById(R.id.admin_home_progress_bar);

        map.put("positive", 1);
        map.put("direct_contact", 2);
        map.put("indirect_contact", 3);
        map.put("safe", 4);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

        addPhoneBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                final String phone = addPhone.getText().toString();
                databaseReference.child(phone).child("status").setValue("positive");

                update_status(phone);
            }
        });

        searchPhoneBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(AdminHomeActivity.this, DisplayActivity.class);
                intent.putExtra("phone", searchPhone.getText().toString());
                startActivity(intent);
            }
        });
    }

    private void setStatus(final String phone, final int status)
    {
        // change if it's of high priority
        databaseReference.child(phone).child("status").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {
                            String myCurrentStatus = dataSnapshot.getValue(String.class);
                            int myPrio = -1;
                            if(map.containsKey(myCurrentStatus))
                            {
                                myPrio = map.get(myCurrentStatus);
                            }
                            if((status==3 || status==2) && myPrio==4)
                            {
                                databaseReference.child(phone).child("status").setValue("indirect_contact");
                            }
                            else if(status==1 && myPrio>2)
                            {
                                databaseReference.child(phone).child("status").setValue("direct_contact");
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                }
        );
    }

    private void update_status(String phone)
    {
        // use dfs
        visitedMap = new HashMap<>();
        find_contacts(new UserTreeObject(-1, -1, -1, phone, "00", 0));
    }

    private void find_contacts(final UserTreeObject user)
    {
        databaseReference.child(user.getPhone()).addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    for(DataSnapshot Year : dataSnapshot.getChildren())
                    {
                        String s = Year.getKey();
                        if(s==null)
                            break;
                        yd = convertToInt(s)-user.getYear();
                        if(yd>=0)
                        {
                            for (DataSnapshot Month : Year.getChildren())
                            {
                                s = Month.getKey();
                                if(s==null)
                                    break;
                                md = convertToInt(s)-user.getMonth();
                                if(yd>0 || md>=0 )
                                {
                                    for (DataSnapshot Day : Month.getChildren())
                                    {
                                        s = Day.getKey();
                                        if(s==null)
                                            break;
                                        dd = convertToInt(s)-user.getDay();
                                        if(yd>0 || md>0 || dd>=0)
                                        {
                                            for (DataSnapshot ph : Day.getChildren())
                                            {
                                                 UserTreeObject object = new UserTreeObject(convertToInt(Year.getKey()), convertToInt(Month.getKey()),
                                                        convertToInt(Day.getKey()), ph.getKey(),
                                                        ph.getValue(String.class), user.getLevel()+1);
                                                 Log.i("findContacts", object.getLevel()+" : "+object.getPhone());
                                                 if(visitedMap.containsKey(object.getPhone()))
                                                 {
                                                     if( visitedMap.get(object.getPhone())>object.getLevel())
                                                     {
                                                         visitedMap.put(object.getPhone(), object.getLevel());
                                                         setStatus(object.getPhone(), object.getLevel());
                                                     }
                                                 }
                                                 else
                                                 {
                                                     visitedMap.put(object.getPhone(), object.getLevel());
                                                     setStatus(object.getPhone(), object.getLevel());
                                                     find_contacts(object);
                                                 }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if(user.getLevel()==0)
                    {
                        progressBar.setVisibility(View.INVISIBLE);
                        Utilities.showMessage(AdminHomeActivity.this, "Result", "Status of patient changed successfully");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            { }
        });
    }

    public int convertToInt(String s)
    {
        int n=0;
        for(int i=0; i<s.length(); i++)
        {
            n = n*10 + s.charAt(i)-'0';
        }
        return n;
    }
}

//q = new LinkedList<>();
//        q.add(new UserTreeObject(-1, -1, -1, phone, "12:12", 0));
//        UserTreeObject user;
//        while(!q.isEmpty())
//        {
//        user = q.remove();
//        if(!visitedMap.containsKey(user.getPhone()))
//        {
//        visitedMap.put(user.getPhone(), 1);
//        find_contacts(user);
//        Log.i("Admin", user.getLevel() + " : "+ user.getPhone());
//        if(user.getLevel()==1)
//        {
//        setStatus(user.getPhone(), "direct_contact");
//        }
//        else if(user.getLevel()>1)
//        {
//        setStatus(user.getPhone(), "indirect_contact");
//        }
//        }
//        try {
//        Thread.sleep(5000);
//        } catch (InterruptedException e) {
//        e.printStackTrace();
//        }
//        }
