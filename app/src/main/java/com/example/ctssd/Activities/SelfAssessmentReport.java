package com.example.ctssd.Activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import com.example.ctssd.R;
import com.example.ctssd.Utils.Utilities;

import java.util.Objects;

public class SelfAssessmentReport extends AppCompatActivity implements View.OnClickListener {

    private CheckBox cold, fever, soreThroat, breathing, diabetes, hypertension, interacted, healthcareWorker;
    private RadioGroup profession, age, travel;
    private Button submit;
    private int riskFromReport;
    private boolean isRiskyProfession, isTravel, isAge;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_assessment_report);

        cold = findViewById(R.id.id_selfAssess_symptoms_cold);
        fever = findViewById(R.id.id_selfAssess_symptoms_fever);
        soreThroat = findViewById(R.id.id_selfAssess_symptoms_soreThroat);
        breathing = findViewById(R.id.id_selfAssess_symptoms_difficultyBreathing);
        diabetes = findViewById(R.id.id_selfAssess_disease_diabetes);
        hypertension = findViewById(R.id.id_selfAssess_disease_hypertension);
        interacted = findViewById(R.id.id_selfAssess_status_interacted);
        healthcareWorker = findViewById(R.id.id_selfAssess_status_healthcareWorker);
        profession = findViewById(R.id.id_selfAssess_profession);
        age = findViewById(R.id.id_selfAssess_age);
        travel = findViewById(R.id.id_selfAssess_travel);
        submit = findViewById(R.id.id_selfAssess_submitBTN);
        progressBar = findViewById(R.id.progress_bar_selfAssess);

        riskFromReport = 0;
        isRiskyProfession = false;

        cold.setOnClickListener(this);
        fever.setOnClickListener(this);
        soreThroat.setOnClickListener(this);
        breathing.setOnClickListener(this);
        diabetes.setOnClickListener(this);
        hypertension.setOnClickListener(this);
        interacted.setOnClickListener(this);
        healthcareWorker.setOnClickListener(this);

        //loadPreviousValues();
        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        boolean isAlreadySubmitted = settings.getBoolean("isAlreadySubmitted", false);
        if(isAlreadySubmitted)
        {
            String lastSumbit = settings.getString("lastSumbit", "25 May");
            Utilities.showMessage(this, "Reminder", "You have already sumbitted the form on "+lastSumbit);
        }

        profession.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if(checkedId!=R.id.id_selfAssess_profession_other)
                    isRiskyProfession = true;
                else
                    isAge = false;
            }
        });

        age.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if(checkedId!=R.id.id_selfAssess_age_inBetween)
                    isAge = true;
                else
                    isAge = false;
            }
        });

        travel.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if(checkedId!=R.id.id_selfAssess_countries_no)
                    isTravel = true;
                else
                    isTravel = false;
            }
        });

        submit.setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.id_selfAssess_symptoms_cold:
                if(cold.isChecked())
                {
                    riskFromReport += 1;
                }
                else
                {
                    riskFromReport -= 1;
                }
                break;

            case R.id.id_selfAssess_symptoms_fever:
                if(fever.isChecked())
                {
                    riskFromReport += 1;
                }
                else
                {
                    riskFromReport -= 1;
                }
                break;

            case R.id.id_selfAssess_symptoms_soreThroat:
                if(soreThroat.isChecked())
                {
                    riskFromReport += 1;
                }
                else
                {
                    riskFromReport -= 1;
                }
                break;

            case R.id.id_selfAssess_symptoms_difficultyBreathing:
                if(breathing.isChecked())
                {
                    riskFromReport += 1;
                }
                else
                {
                    riskFromReport -= 1;
                }
                break;

            case R.id.id_selfAssess_disease_diabetes:
                if(diabetes.isChecked())
                {
                    riskFromReport += 1;
                }
                else
                {
                    riskFromReport -= 1;
                }
                break;

            case R.id.id_selfAssess_disease_hypertension:
                if(hypertension.isChecked())
                {
                    riskFromReport += 1;
                }
                else
                {
                    riskFromReport -= 1;
                }
                break;

            case R.id.id_selfAssess_status_interacted:
                if(interacted.isChecked())
                {
                    riskFromReport += 5;
                }
                else
                {
                    riskFromReport -= 5;
                }
                break;

            case R.id.id_selfAssess_status_healthcareWorker:
                if(healthcareWorker.isChecked())
                {
                    riskFromReport += 5;
                }
                else
                {
                    riskFromReport -= 5;
                }
                break;

            case R.id.id_selfAssess_submitBTN:
                if(isRiskyProfession)
                    riskFromReport += 5;
                if(isAge)
                    riskFromReport += 5;
                if(isTravel)
                    riskFromReport += 1;

                Intent intent2 = new Intent("ACTION_UPDATE_RISK");
                intent2.putExtra("riskFromReport", riskFromReport);
                sendBroadcast(intent2);
                Log.i("SelfAssess", "Broadcast sent to update risk");
                progressBar.setVisibility(View.INVISIBLE);
                SelfAssessmentReport.this.onBackPressed();
        }
        Log.i("SelfAssess", "RiskFromReport :"+riskFromReport);
    }

    private void loadPreviousValues()
    {
        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);

        int preProfession = settings.getInt("preProfession", -1);
        int preAge = settings.getInt("preAge", -1);
        int preTravel = settings.getInt("preTravel", -1);

        if(preProfession!=-1)
        {
            if(preProfession<6)
                isRiskyProfession = true;
            switch (preProfession) {
                case 1:
                    findViewById(R.id.id_selfAssess_profession_doctor).setEnabled(true);
                    break;
                case 2:
                    findViewById(R.id.id_selfAssess_profession_delivery).setEnabled(true);
                    break;
                case 3:
                    findViewById(R.id.id_selfAssess_profession_wholeseller).setEnabled(true);
                    break;
                case 4:
                    findViewById(R.id.id_selfAssess_profession_chemist).setEnabled(true);
                    break;
                case 5:
                    findViewById(R.id.id_selfAssess_profession_police).setEnabled(true);
                    break;
                case 6:
                    findViewById(R.id.id_selfAssess_profession_other).setEnabled(true);
                    break;
            }
        }

        if(preTravel!=-1)
        {
            if(preTravel==1)
                isTravel = true;
            switch (preTravel)
            {
                case 1: findViewById(R.id.id_selfAssess_countries_yes).setEnabled(true); break;
                case 2: findViewById(R.id.id_selfAssess_countries_no).setEnabled(true); break;
            }
        }

        if(preAge!=-1)
        {
            if(preAge!=2)
                isAge = true;
            switch (preAge)
            {
                case 1: findViewById(R.id.id_selfAssess_age_below10).setEnabled(true); break;
                case 2: findViewById(R.id.id_selfAssess_age_inBetween).setEnabled(true); break;
                case 3: findViewById(R.id.id_selfAssess_age_above60).setEnabled(true); break;
            }
        }
    }
}
