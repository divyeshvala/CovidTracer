package com.example.ctssd.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import android.net.Uri;
import android.os.Bundle;
import com.example.ctssd.Activities.Fragments.Tab1;
import com.example.ctssd.Activities.Fragments.Tab2;
import com.example.ctssd.Activities.Fragments.Tab3;
import com.example.ctssd.R;
import com.example.ctssd.Utils.Utilities;
import com.google.android.material.tabs.TabLayout;

public class Main2Activity extends AppCompatActivity implements Tab1.OnFragmentInteractionListener, Tab2.OnFragmentInteractionListener, Tab3.OnFragmentInteractionListener {

    public static String myMacAdd = "-1";
    private TabLayout tabLayout;
    private PagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        myMacAdd = getIntent().getStringExtra("myMacAdd");

        new Thread( new Runnable() { @Override public void run() {
            Utilities utilities = new Utilities();
            utilities.uploadDataToCloud(Main2Activity.this);
            } } ).start();

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
}
