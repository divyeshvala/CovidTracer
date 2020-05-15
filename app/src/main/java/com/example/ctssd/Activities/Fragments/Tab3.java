package com.example.ctssd.Activities.Fragments;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.example.ctssd.R;
import com.example.ctssd.Utils.DatabaseHelper;
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

public class Tab3 extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private TextView numbers;
    private BarChart barChart1, barChart2;
    private RadioGroup radioGroup ;
    DatabaseHelper myDb;
    List<BarEntry> barEntries1, barEntries2 ;
    String[] xAxisLabels1, xAxisLabels2;
    boolean isFirstTime;

    private static int preTodayContacts=0;
    private View rootVar;

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public Tab3() {
        // Required empty public constructor
    }

    public static Tab3 newInstance(String param1, String param2)
    {
        Tab3 fragment = new Tab3();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        final View root =inflater.inflate(R.layout.fragment_tab3, container, false);
        rootVar = root;
        barChart1 = (BarChart) root.findViewById(R.id.barchart1);
        barChart2 = (BarChart) root.findViewById(R.id.barchart2);
        radioGroup = (RadioGroup) root.findViewById(R.id.id_radioGroup);

        myDb = new DatabaseHelper(getActivity());
        barEntries1 = new ArrayList<>();
        barEntries2 = new ArrayList<>();
        xAxisLabels1 = new String[8];
        xAxisLabels2 = new String[8];
        isFirstTime = true;

        fillBarEntries();
        Cursor cursor = myDb.getAllData();
        if(cursor!=null) {
            preTodayContacts = cursor.getCount();
            barEntries1.add(new BarEntry(7, preTodayContacts));
            cursor.close();
        }
        else
            barEntries1.add(new BarEntry(7, 0));

        Calendar calendar = Calendar.getInstance();
        xAxisLabels1[7] = calendar.get(Calendar.DAY_OF_MONTH)+"-"+calendar.get((Calendar.MONTH));

        displayGraph();

        IntentFilter intentFilter = new IntentFilter("ACTION_update_contacts_today");
        getActivity().registerReceiver(receiver, intentFilter);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if( checkedId == root.findViewById(R.id.id_last7Days).getId() )
                {
                    barChart2.setVisibility(View.INVISIBLE);
                    barChart1.setVisibility(View.VISIBLE);
                }
                else
                {
                    barChart1.setVisibility(View.INVISIBLE);
                    barChart2.setVisibility(View.VISIBLE);
                    if(isFirstTime)
                    {
                        showGraph(barChart2, barEntries2, xAxisLabels2);
                        isFirstTime = false;
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
        Cursor mCursor = myDb.getAllDataTable2();
        if(mCursor==null)
            return;
        int i=1, j=-13;
        barEntries2.add(new BarEntry(0, 0));
        xAxisLabels2[0] = "";
        while( i<8 && mCursor.moveToNext())
        {
            barEntries2.add(new BarEntry(i, mCursor.getInt(1)));
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, j);
            Date dateBeforeNDays = cal.getTime();
            cal.setTime(dateBeforeNDays);
            xAxisLabels2[i]= cal.get(Calendar.DAY_OF_MONTH)+"-"+cal.get(Calendar.MONTH);
            i++;
            j++;
        }

        barEntries1.add(new BarEntry(0, 0));

        xAxisLabels1[0] = "";
        i=1;
        while( mCursor.moveToNext() && i<7)
        {
            barEntries1.add(new BarEntry(i, mCursor.getInt(1)));
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, j);
            Date dateBeforeNDays = cal.getTime();
            cal.setTime(dateBeforeNDays);
            xAxisLabels1[i]= cal.get(Calendar.DAY_OF_MONTH)+"-"+cal.get(Calendar.MONTH);
            i++;
            j++;
        }
        mCursor.close();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            Log.i("Tab2 Receiver", "updating graph");

            Cursor cursor = myDb.getAllData();
            if(cursor!=null && preTodayContacts!=cursor.getCount())
            {
                preTodayContacts = cursor.getCount();
                cursor.close();
                barEntries1.add(7, new BarEntry(7, preTodayContacts));
                barChart1.clear();
                barChart1 = rootVar.findViewById(R.id.barchart1);
                barChart1.clear();
                displayGraph();
                //barChart1.notifyDataSetChanged();
            }
        }
    };

    private void displayGraph()
    {
        showGraph(barChart1, barEntries1, xAxisLabels1);
    }

    private void showGraph(BarChart barChart, List<BarEntry> barEntries, String[] values)
    {
        BarDataSet barDataSet = new BarDataSet(barEntries, "Contacts");
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
        xAxis.setAxisMinimum(0);

        Description description = new Description();
        description.setText("Daily contacts");
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
