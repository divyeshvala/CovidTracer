package com.example.ctssd.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import com.example.ctssd.R;

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
}
