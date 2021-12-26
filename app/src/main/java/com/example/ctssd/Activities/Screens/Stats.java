package com.example.ctssd.Activities.Screens;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.example.ctssd.R;
import com.example.ctssd.dao.ContactDao;
import com.example.ctssd.dao.DailyStatDao;
import com.example.ctssd.model.DailyStat;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class Stats extends Fragment {

    private TextView yAxisTitle;
    private BarChart barChart;
    private RadioButton last7Days, sevenDaysAgo;
    private RadioGroup radioGroup ;
    private DailyStatDao dailyStatDao;
    private ContactDao contactDao;
    private List<BarEntry> barEntriesContactsLast7Days, barEntriesContacts7DaysAgo;
    private List<BarEntry> barEntriesRiskLast7Days, barEntriesRisk7DaysAgo;
    private String[] xAxisLabelsLast7Days, xAxisLabels7DaysAgo;
    private Button contactsGraphBTN, riskFactorGraphBTN;
    private String selectedBTN;

    private static int preTodayContacts=0;
    //private View rootVar;

    private OnFragmentInteractionListener mListener;

    public Stats() {
    }

    public static Stats newInstance(String param1, String param2)
    {
        Stats fragment = new Stats();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        final View root =inflater.inflate(R.layout.fragment_tab3, container, false);
        barChart = root.findViewById(R.id.barChart);
        last7Days = root.findViewById(R.id.id_last7Days);
        sevenDaysAgo = root.findViewById(R.id.id_7DaysAgo);
        radioGroup = root.findViewById(R.id.id_radioGroup);
        contactsGraphBTN = root.findViewById(R.id.id_contactsGraphBTN);
        riskFactorGraphBTN = root.findViewById(R.id.id_riskGraphBTN);
        yAxisTitle = root.findViewById(R.id.id_YAxisTitle);

        contactsGraphBTN.setEnabled(false);
        selectedBTN = "contacts";

        contactDao = new ContactDao(getActivity());
        dailyStatDao = new DailyStatDao(getActivity());
        barEntriesContactsLast7Days = new ArrayList<>();
        barEntriesContacts7DaysAgo = new ArrayList<>();
        barEntriesRiskLast7Days = new ArrayList<>();
        barEntriesRisk7DaysAgo = new ArrayList<>();

        xAxisLabelsLast7Days = new String[8];
        xAxisLabels7DaysAgo = new String[8];

        fillBarEntries();

        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", getActivity().MODE_PRIVATE);
        barEntriesRiskLast7Days.add(new BarEntry(7, settings.getInt("myRiskIndex", 0)));
        preTodayContacts = contactDao.getCount();
        barEntriesContactsLast7Days.add(new BarEntry(7, preTodayContacts));

        Calendar calendar = Calendar.getInstance();
        xAxisLabelsLast7Days[7] = calendar.get(Calendar.DAY_OF_MONTH)+"-"+(calendar.get((Calendar.MONTH))+1);
        showGraph(barChart, barEntriesContactsLast7Days, xAxisLabelsLast7Days, "Contacts");
        
        IntentFilter intentFilter = new IntentFilter("ACTION_UPDATE_CONTACTS");
        Objects.requireNonNull(getActivity()).registerReceiver(receiver, intentFilter);

        IntentFilter intentFilter1 = new IntentFilter("ACTION_UPDATE_RISK_GRAPH");
        Objects.requireNonNull(getActivity()).registerReceiver(receiver, intentFilter1);

        contactsGraphBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactsGraphBTN.setEnabled(false);
                riskFactorGraphBTN.setEnabled(true);
                if(!selectedBTN.equals("contacts"))
                {
                    yAxisTitle.setText("C\nO\nN\nT\nA\nC\nT\nS");
                    selectedBTN = "contacts";
                    last7Days.setChecked(true);
                    showGraph(barChart, barEntriesContactsLast7Days, xAxisLabelsLast7Days, "Contacts");
                }
            }
        });

        riskFactorGraphBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactsGraphBTN.setEnabled(true);
                riskFactorGraphBTN.setEnabled(false);
                if(!selectedBTN.equals("riskFactor"))
                {
                    yAxisTitle.setText("R\nI\nS\nK\n\nF\nA\nC\nT\nO\nR");
                    selectedBTN = "riskFactor";
                    last7Days.setChecked(true);
                    showGraph(barChart, barEntriesRiskLast7Days, xAxisLabelsLast7Days, "Risk factor");
                }
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if(selectedBTN.equals("contacts"))
                {
                    yAxisTitle.setText("C\nO\nN\nT\nA\nC\nT\nS");
                    if( checkedId == root.findViewById(R.id.id_last7Days).getId() )
                    {
                        showGraph(barChart, barEntriesContactsLast7Days, xAxisLabelsLast7Days, "Contacts");
                    }
                    else
                    {
                        showGraph(barChart, barEntriesContacts7DaysAgo, xAxisLabels7DaysAgo, "Contacts");
                    }
                }
                else
                {
                    yAxisTitle.setText("R\nI\nS\nK\n\nF\nA\nC\nT\nO\nR");
                    if( checkedId == root.findViewById(R.id.id_last7Days).getId() )
                    {
                        showGraph(barChart, barEntriesRiskLast7Days, xAxisLabelsLast7Days, "Risk factor");
                    }
                    else
                    {
                        showGraph(barChart, barEntriesRisk7DaysAgo, xAxisLabels7DaysAgo, "Risk factor");
                    }
                }
            }
        });
        return root;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener
    {
        void onFragmentInteraction(Uri uri);
    }

    private void fillBarEntries()
    {
        List<DailyStat> dailyStatList = dailyStatDao.getAll();
        if(dailyStatList.size()==0)
            return;
        int i=1, j=-13;
        barEntriesContacts7DaysAgo.add(new BarEntry(0, 0));
        barEntriesRisk7DaysAgo.add(new BarEntry(0, 0));

        xAxisLabels7DaysAgo[0] = "";
        while( i<8)
        {
            barEntriesContacts7DaysAgo.add(new BarEntry(i, dailyStatList.get(i).getContactsCount()));
            barEntriesRisk7DaysAgo.add(new BarEntry(i, dailyStatList.get(i).getRisk()));
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, j);
            Date dateBeforeNDays = cal.getTime();
            cal.setTime(dateBeforeNDays);
            xAxisLabels7DaysAgo[i]= cal.get(Calendar.DAY_OF_MONTH)+"-"+(cal.get((Calendar.MONTH))+1);
            i++;
            j++;
        }

        barEntriesContactsLast7Days.add(new BarEntry(0, 0));
        barEntriesRiskLast7Days.add(new BarEntry(0, 0));

        xAxisLabelsLast7Days[0] = "";
        i=1;
        while(i<7)
        {
            barEntriesContactsLast7Days.add(new BarEntry(i, dailyStatList.get(i).getContactsCount()));
            barEntriesRiskLast7Days.add(new BarEntry(i, dailyStatList.get(i).getRisk()));
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, j);
            Date dateBeforeNDays = cal.getTime();
            cal.setTime(dateBeforeNDays);
            xAxisLabelsLast7Days[i]= cal.get(Calendar.DAY_OF_MONTH)+"-"+(cal.get((Calendar.MONTH))+1);
            i++;
            j++;
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            Log.i("Tab3 Receiver", "updating graph");
            String action = intent.getAction();
            if(action!=null && action.equals("ACTION_UPDATE_CONTACTS"))
            {
                int contactsCount = contactDao.getCount();
                if(preTodayContacts!=contactsCount)
                {
                    SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
                    preTodayContacts = contactsCount + settings.getInt("contactsTodayPenalty", 0);
                    barEntriesContactsLast7Days.remove(7);
                    barEntriesContactsLast7Days.add(7, new BarEntry(7, preTodayContacts));
                    if(selectedBTN.equals("contacts") && last7Days.isChecked())
                    {
                        Log.i("TAB3", "New contact added");
                        barChart.notifyDataSetChanged();
                    }
                }
            }
            else if(action!=null && action.equals("ACTION_UPDATE_RISK_GRAPH"))
            {
                int risk = intent.getIntExtra("riskIndex", 0);
                barEntriesRiskLast7Days.remove(7);
                barEntriesRiskLast7Days.add(7, new BarEntry(7, risk));
                if(selectedBTN.equals("riskFactor") && last7Days.isChecked())
                {
                    Log.i("TAB3", "Risk has been updated.");
                    barChart.notifyDataSetChanged();
                }
            }
        }
    };

    private void showGraph(BarChart barChart, List<BarEntry> barEntries, String[] values, String label)
    {
        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.9f);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setVisibility(View.VISIBLE);
        barChart.animateY(1000);
        barChart.setData(barData);
        barChart.setFitBars(true);
        barChart.setDrawGridBackground(true);


        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new MyXAxisValueFormatter(values));
        xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);
        xAxis.setGranularity(1);
        //xAxis.setCenterAxisLabels(true);
        xAxis.setTextSize(13);
        xAxis.setAxisMinimum(0);

        Description description = new Description();
        description.setText("");
        description.setTextSize(14);
        barChart.setDescription(description);
        barChart.invalidate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Objects.requireNonNull(getActivity()).unregisterReceiver(receiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public class MyXAxisValueFormatter extends IndexAxisValueFormatter {
        private String[] list;

        private MyXAxisValueFormatter(String[] list)
        {
            this.list = list;
        }

        @Override
        public String getFormattedValue(float value) {
            return list[(int)value];
        }
    }
}
