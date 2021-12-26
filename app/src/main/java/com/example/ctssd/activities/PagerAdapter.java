package com.example.ctssd.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.ctssd.activities.screens.Display;
import com.example.ctssd.activities.screens.Dashboard;
import com.example.ctssd.activities.screens.Stats;
import com.example.ctssd.R;

import java.util.ArrayList;
import java.util.List;

public class PagerAdapter extends FragmentStatePagerAdapter {

    private int mNumOfTabs;
    private int[] tabIcons = {
            R.drawable.ic_home_black_24dp,
            R.drawable.ic_dashboard_black_24dp,
            R.drawable.icons_graph
    };
    private String[] tabNames = {
            "Home",
            "Stats",
            "Graph"
    };
    private List<String> fragmentTitleList = new ArrayList<>();

    public PagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
        this.mNumOfTabs = behavior;
    }

    @NonNull
    @Override
    public Fragment getItem(int position)
    {
        switch (position)
        {
            case 0:
                return new Display();
            case 1:
                return  new Dashboard();
            case 2:
                return  new Stats();
            default:
                return null;
        }
    }

    public View getTabView(Context context,int position) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_tab, null);
        TextView tabTextView = view.findViewById(R.id.tabTextView);
        ImageView tabImageView = view.findViewById(R.id.tabImageView);
        tabImageView.setImageResource(tabIcons[position]);
        tabTextView.setText(tabNames[position]);
        return view;
    }

    public View getSelectedTabView(Context context, int position)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_tab, null);
        TextView tabTextView = view.findViewById(R.id.tabTextView);
        ImageView tabImageView = view.findViewById(R.id.tabImageView);
        tabImageView.setImageResource(tabIcons[position]);
        tabTextView.setText(tabNames[position]);
        tabTextView.setTextSize(18); // for big text, increase text size

        LinearLayout linearLayout = view.findViewById(R.id.id_tabLinearLayout);

        return view;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
