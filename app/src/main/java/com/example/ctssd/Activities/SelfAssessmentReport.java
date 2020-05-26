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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.example.ctssd.R;
import com.example.ctssd.Utils.Utilities;

import java.io.File;
import java.util.Objects;

public class SelfAssessmentReport extends AppCompatActivity implements View.OnClickListener {

    private CheckBox cold, fever, soreThroat, breathing, diabetes, hypertension, interacted, healthcareWorker;
    private RadioGroup profession, age, travel;
    private Button submit;
    private int riskFromReport;
    private boolean isRiskyProfession, isTravel, isAge;
    private ProgressBar progressBar;
    private SharedPreferences settings;
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

        settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);

        cold.setOnClickListener(this);
        fever.setOnClickListener(this);
        soreThroat.setOnClickListener(this);
        breathing.setOnClickListener(this);
        diabetes.setOnClickListener(this);
        hypertension.setOnClickListener(this);
        interacted.setOnClickListener(this);
        healthcareWorker.setOnClickListener(this);

        loadPreviousValues();

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

        SharedPreferences.Editor editor = settings.edit();

        switch (v.getId())
        {
            case R.id.id_selfAssess_symptoms_cold:
                if(cold.isChecked())
                {
                    riskFromReport += 1;
                    editor.putBoolean("cold", true);
                }
                else
                {
                    riskFromReport -= 1;
                    editor.putBoolean("cold", false);
                }
                break;

            case R.id.id_selfAssess_symptoms_fever:
                if(fever.isChecked())
                {
                    riskFromReport += 1;
                    editor.putBoolean("fever",true);
                }
                else
                {
                    riskFromReport -= 1;
                    editor.putBoolean("fever",false);
                }
                break;

            case R.id.id_selfAssess_symptoms_soreThroat:
                if(soreThroat.isChecked())
                {
                    riskFromReport += 1;
                    editor.putBoolean("sorethroat",true);
                }
                else
                {
                    riskFromReport -= 1;
                    editor.putBoolean("sorethroat",false);
                }
                break;

            case R.id.id_selfAssess_symptoms_difficultyBreathing:
                if(breathing.isChecked())
                {
                    riskFromReport += 1;
                    editor.putBoolean("breathing",true);
                }
                else
                {
                    riskFromReport -= 1;
                    editor.putBoolean("breathing",false);
                }
                break;

            case R.id.id_selfAssess_disease_diabetes:
                if(diabetes.isChecked())
                {
                    riskFromReport += 1;
                    editor.putBoolean("diabetes",true);
                }
                else
                {
                    riskFromReport -= 1;
                    editor.putBoolean("diabetes",false);
                }
                break;

            case R.id.id_selfAssess_disease_hypertension:
                if(hypertension.isChecked())
                {
                    riskFromReport += 1;
                    editor.putBoolean("hypertension",true);
                }
                else
                {
                    riskFromReport -= 1;
                    editor.putBoolean("hypertension",false);
                }
                break;

            case R.id.id_selfAssess_status_interacted:
                if(interacted.isChecked())
                {
                    riskFromReport += 5;
                    editor.putBoolean("interacted",true);
                }
                else
                {
                    riskFromReport -= 5;
                    editor.putBoolean("interacted",false);
                }
                break;

            case R.id.id_selfAssess_status_healthcareWorker:
                if(healthcareWorker.isChecked())
                {
                    riskFromReport += 5;
                    editor.putBoolean("healthcareworker",true);
                }
                else
                {
                    riskFromReport -= 5;
                    editor.putBoolean("healthcareworker",false);
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

        editor.putInt("preProfession",profession.getCheckedRadioButtonId());
        editor.putInt("preAge",age.getCheckedRadioButtonId());
        editor.putInt("preTravel",travel.getCheckedRadioButtonId());
        editor.apply();

        Log.i("SelfAssess", "RiskFromReport :"+riskFromReport);
    }

    private void loadPreviousValues()
    {
        int preProfession = settings.getInt("preProfession", -1);
        int preAge = settings.getInt("preAge", -1);
        int preTravel = settings.getInt("preTravel", -1);

        if(settings.getBoolean("cold",false))
        {    cold.setChecked(true);  riskFromReport+=1; }
        if(settings.getBoolean("fever",false))
        {    fever.setChecked(true);  riskFromReport+=1; }
        if(settings.getBoolean("sorethroat",false))
        {    soreThroat.setChecked(true);  riskFromReport+=1; }
        if(settings.getBoolean("breathing",false))
        {    breathing.setChecked(true);  riskFromReport+=1; }
        if(settings.getBoolean("diabetes",false))
        {    diabetes.setChecked(true);  riskFromReport+=1; }
        if(settings.getBoolean("hypertension",false))
        {    hypertension.setChecked(true);  riskFromReport+=1; }
        if(settings.getBoolean("interacted",false))
        {    interacted.setChecked(true);  riskFromReport+=5; }
        if(settings.getBoolean("healthcareworker",false))
        {    healthcareWorker.setChecked(true);  riskFromReport+=5; }

        if(preProfession!=-1)
        {
            if(preProfession!=R.id.id_selfAssess_profession_other)
                isRiskyProfession = true;
            RadioButton profans = findViewById(preProfession);
            profans.setChecked(true);
        }

        if(preTravel!=-1)
        {
            if(preTravel==R.id.id_selfAssess_countries_yes)
                isTravel = true;
            RadioButton travans = findViewById(preTravel);
            travans.setChecked(true);
        }

        if(preAge!=-1)
        {
            if(preAge!=R.id.id_selfAssess_age_inBetween)
                isAge = true;
            RadioButton agans = findViewById(preAge);
            agans.setChecked(true);
        }
    }
}