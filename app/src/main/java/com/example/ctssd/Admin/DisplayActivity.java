package com.example.ctssd.Admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.example.ctssd.R;
import com.example.ctssd.Utils.Adapter;
import com.example.ctssd.Utils.UserObject;
import com.example.ctssd.Utils.UserTreeObject;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class DisplayActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ArrayList<UserObject> directContactsList, indirectContactsList;
    private HashMap<String, String> visitedMap;
    private RecyclerView recyclerViewDirect, recyclerViewIndirect;
    private Adapter adapterDirect, adapterIndirect;

    private int yd=-1, md=-1, dd=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        progressBar = findViewById(R.id.displayContacts_progress_bar);
        recyclerViewDirect = (RecyclerView) findViewById(R.id.id_directContacts_recyclerView);
        recyclerViewIndirect = (RecyclerView) findViewById(R.id.id_indirectContacts_recyclerView);


        String phone = getIntent().getStringExtra("phone");

        directContactsList = new ArrayList<>();
        indirectContactsList = new ArrayList<>();
        adapterDirect = new Adapter(DisplayActivity.this, directContactsList);
        adapterIndirect = new Adapter(DisplayActivity.this, indirectContactsList);
        setupRecyclerView(recyclerViewDirect, directContactsList, adapterDirect);
        setupRecyclerView(recyclerViewIndirect, indirectContactsList, adapterIndirect);

        progressBar.setVisibility(View.VISIBLE);
        visitedMap = new HashMap<>();

        visitedMap.put(phone, "yes");
        getContacts(new UserTreeObject(-1, -1, -1, phone, "00-00-00", 0), 0);

        progressBar.setVisibility(View.INVISIBLE);
    }

    private void setupRecyclerView(RecyclerView recyclerView, ArrayList<UserObject> lst, Adapter adapter)
    {
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(false);
        LinearLayoutManager questionListLayoutManager = new LinearLayoutManager(DisplayActivity.this);
        recyclerView.setLayoutManager(questionListLayoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void getContacts(final UserTreeObject object, final int i)
    {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        databaseReference.child(object.getPhone()).addListenerForSingleValueEvent(new ValueEventListener()
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
                        yd = convertToInt(s)-object.getYear();
                        if(yd>=0)
                        {
                            for (DataSnapshot Month : Year.getChildren())
                            {
                                s = Month.getKey();
                                if(s==null)
                                    break;
                                md = convertToInt(s)-object.getMonth();
                                if(yd>0 || md>=0 )
                                {
                                    for (DataSnapshot Day : Month.getChildren())
                                    {
                                        s = Day.getKey();
                                        if(s==null)
                                            break;
                                        dd = convertToInt(s)-object.getDay();
                                        if(yd>0 || md>0 || dd>=0)
                                        {
                                            for (DataSnapshot ph : Day.getChildren()) {
                                                Log.i("display", object.getPhone());
                                                //contactList.add(new UserTreeObject(convertToInt(Year.getKey()), convertToInt(Month.getKey()), convertToInt(Day.getKey()), ph.getKey(), ph.getValue(String.class)));
                                                if (!visitedMap.containsKey(ph.getKey()))
                                                {
                                                    visitedMap.put(ph.getKey(), "yes");
                                                    getContacts(new UserTreeObject(convertToInt(Year.getKey()), convertToInt(Month.getKey()), convertToInt(Day.getKey()), ph.getKey(), ph.getValue(String.class), 0), i+1);
                                                    if(i==0)
                                                    {
                                                        directContactsList.add(new UserObject(ph.getKey()
                                                                , Day.getKey()+"-"+Month.getKey()+"-"+Year.getKey()+"  "+ph.getValue(String.class)));
                                                        adapterDirect.notifyDataSetChanged();
                                                    }
                                                    else if(i>0)
                                                    {
                                                        indirectContactsList.add(new UserObject(ph.getKey()
                                                                , Day.getKey()+"-"+Month.getKey()+"-"+Year.getKey()+"  "+ph.getValue(String.class)));
                                                        adapterIndirect.notifyDataSetChanged();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
