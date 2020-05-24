package com.example.ctssd.Activities;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.ctssd.Activities.Fragments.Tab1;
import com.example.ctssd.Activities.Fragments.Tab2;
import com.example.ctssd.Activities.Fragments.Tab3;
import com.example.ctssd.R;
import com.example.ctssd.Utils.Utilities;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

public class Main2Activity extends AppCompatActivity implements Tab1.OnFragmentInteractionListener, Tab2.OnFragmentInteractionListener, Tab3.OnFragmentInteractionListener {

    public static String myMacAdd = "-1";
    public static String myPhoneNumber;
    private TabLayout tabLayout;
    private PagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        myPhoneNumber = settings.getString("myPhoneNumber", "NA");

        IntentFilter intentFilter3 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(turnOnBluetooth, intentFilter3);

        tabLayout = (TabLayout)findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager)findViewById(R.id.pager);
        adapter = new PagerAdapter(getSupportFragmentManager(),tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(1);   // Default tab is set to 1
        viewPager.setOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            assert tab != null;
            tab.setCustomView(null);
            tab.setCustomView(adapter.getTabView(Main2Activity.this, i));
        }
        highLightCurrentTab(1);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                highLightCurrentTab(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void highLightCurrentTab(int position)
    {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            assert tab != null;
            tab.setCustomView(null);
            tab.setCustomView(adapter.getTabView(Main2Activity.this, i));
        }
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        assert tab != null;
        tab.setCustomView(null);
        tab.setCustomView(adapter.getSelectedTabView(Main2Activity.this, position));
    }

    @Override
    public void onFragmentInteraction(Uri uri) { }

        private BroadcastReceiver turnOnBluetooth = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(turnOnBluetooth);
                if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
                {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 41);
                }
            }
    };


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==41)
        {
            Log.i("Tab1", "Inside on activity result :"+resultCode);
            if(BluetoothAdapter.getDefaultAdapter().getState()!=BluetoothAdapter.STATE_ON)
            {
                Log.i("Tab1", "Inside on activity result : BT is off");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 41);
            }
            else
            {
                Log.i("Tab1", "Inside on activity result : BT is on now");
                IntentFilter intentFilter3 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(turnOnBluetooth, intentFilter3);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(turnOnBluetooth);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
