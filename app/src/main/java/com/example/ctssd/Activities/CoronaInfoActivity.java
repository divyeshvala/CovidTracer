package com.example.ctssd.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.ctssd.R;

import java.util.HashMap;

public class CoronaInfoActivity extends AppCompatActivity implements View.OnClickListener {

    private int expanded = -1;

    HashMap<Integer, int[]> layouts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corona_info);

        TextView howItSpreads = findViewById(R.id.id_howitSpreads_title);
        TextView symptoms = findViewById(R.id.id_symptoms_title);
        TextView prevention = findViewById(R.id.id_prevention_title);
        TextView treatments = findViewById(R.id.id_treatment_title);

        layouts = new HashMap<>();

        layouts.put(R.id.id_howitSpreads_title, new int[]{R.id.id_howItSpreads_details_text});
        layouts.put(R.id.id_symptoms_title, new int[]{R.id.id_symptoms_text1, R.id.id_symptoms_text2, R.id.id_symptoms_text3, R.id.id_symptoms_text4});
        layouts.put(R.id.id_prevention_title, new int[]{R.id.id_preventions_text1, R.id.id_preventions_text2});
        layouts.put(R.id.id_treatment_title, new int[]{R.id.id_treatment_text1, R.id.id_treatment_text2, R.id.id_treatment_text3, R.id.id_treatment_text4, R.id.id_treatment_text5});

        howItSpreads.setOnClickListener(this);
        symptoms.setOnClickListener(this);
        prevention.setOnClickListener(this);
        treatments.setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        if(expanded==-1)
        {
            for(int i : layouts.get(v.getId()) )
            {
                findViewById(i).setVisibility(View.VISIBLE);
            }
            expanded = v.getId();
            return;
        }
        if(v.getId()==expanded)
        {
            for(int i : layouts.get(v.getId()) )
            {
                findViewById(i).setVisibility(View.GONE);
            }
            expanded = -1;
        }
        else
        {
            for(int i : layouts.get(expanded) )
            {
                findViewById(i).setVisibility(View.GONE);
            }
            for(int i : layouts.get(v.getId()) )
            {
                findViewById(i).setVisibility(View.VISIBLE);
            }
            expanded = v.getId();
        }
    }
}
