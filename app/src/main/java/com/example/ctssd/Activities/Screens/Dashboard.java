
package com.example.ctssd.Activities.Screens;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.ctssd.Activities.CoronaInfoActivity;
import com.example.ctssd.Activities.MainActivity;
import com.example.ctssd.R;
import com.example.ctssd.Utils.DatabaseHelper;
import com.example.ctssd.Utils.Utilities;
import java.util.Locale;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class Dashboard extends Fragment implements View.OnClickListener {
    private BluetoothAdapter bluetoothAdapter;
    private static final String AppId = "c1t2";
    private static final String TAG = "TAB2";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private OnFragmentInteractionListener mListener;
    private TextView todaysNum;
    private TextView riskIndexText, currentStatus, avgContacts, violationNumText,
            maxContactsText, maxRiskIndexText, avgRiskIndexText, BTOffTimeText,
            maxBTOffTimeText, avgBTOffTimeText;
    private DatabaseHelper myDb;
    private static String currentStatusVar = "null";
    private static int riskIndexVar = -1, past13DaysRiskSum = 0, maxRiskIndexVar = 0, maxContactsVar = 0;
    private static float BTOffTime = 0, maxBTOffTime = 0, past13DaysBTOffSum = 0;
    private static double avgNumVar = 0.0;
    private static int contactsTodayVar = 0, past13DaySum = 0, totalDays = 1,
            violationNumVar = 0;

    public Dashboard() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_tab2, container, false);

        todaysNum = root.findViewById(R.id.id_numStat);
        riskIndexText = root.findViewById(R.id.id_riskIndexText);
        currentStatus = root.findViewById(R.id.id_currentStatus);
        avgContacts = root.findViewById(R.id.id_avgContacts);
        maxContactsText = root.findViewById(R.id.id_maxContactsText);
        maxRiskIndexText = root.findViewById(R.id.id_maxRiskText);
        avgRiskIndexText = root.findViewById(R.id.id_avgRiskText);

        violationNumText = root.findViewById(R.id.id_violationText);
        BTOffTimeText = root.findViewById(R.id.id_bluetoothText);
        maxBTOffTimeText = root.findViewById(R.id.id_maxBluetoothText);
        avgBTOffTimeText = root.findViewById(R.id.id_avgBluetooth);
        RelativeLayout CoronaInfoBTN = root.findViewById(R.id.id_goTo_CoronaInfoActivity);

        ImageView numbersInfo = root.findViewById(R.id.id_contactsTodayInfo);
        ImageView riskIndexInfo = root.findViewById(R.id.id_riskIndexInfo);
        ImageView violationInfo = root.findViewById(R.id.id_violationInfo);
        ImageView bluetoothInfo = root.findViewById(R.id.id_bluetoothInfo);

        numbersInfo.setOnClickListener(this);
        riskIndexInfo.setOnClickListener(this);
        violationInfo.setOnClickListener(this);
        bluetoothInfo.setOnClickListener(this);

        myDb = new DatabaseHelper(getActivity());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        final SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);

        // Every 24 hours work
        final Utilities utilities = new Utilities();
        if (utilities.isTwentyFourHoursOver(getActivity())) {
            int preRiskIndex = settings.getInt("myRiskIndex", 0);
            utilities.TwentyFourHoursWork(getActivity(), preRiskIndex);
            SharedPreferences.Editor mapEditor = settings.edit();
            mapEditor.putInt("fromContactsRiskMax", 0);
            mapEditor.apply();

            riskIndexVar = 0;

            // for starting this app automatically next day.
            //Alarm alarm = new Alarm();
            //alarm.setAlarm(getActivity());

            Cursor cursor = myDb.getAllDataTable3();
            if (cursor != null && cursor.getCount() > 0) {
                if (utilities.isInternetAvailable(getActivity())) {
                    Log.i(TAG, "Uploading data to cloud");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            utilities.uploadDataToCloud(getActivity());
                        }
                    }).start();
                }
            }
        } else {
            riskIndexVar = settings.getInt("myRiskIndex", 0);
        }
        violationNumVar = settings.getInt("crowdNo", 0);
        BTOffTime = settings.getFloat("totalBTOnTime", 0);
        totalDays = settings.getInt("totalDays", 1);  // since the app installation

        // register receiver for broadcast when contact today is changed
        IntentFilter intentFilter = new IntentFilter("ACTION_UPDATE_CONTACTS");
        getActivity().registerReceiver(receiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter("ACTION_UPDATE_RISK");
        getActivity().registerReceiver(receiver, intentFilter2);
        IntentFilter intentFilte = new IntentFilter("ACTION_UPDATE_RISK_HALF_HOUR");
        getActivity().registerReceiver(receiver, intentFilte);

        // Goto about corona information page.
        CoronaInfoBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), CoronaInfoActivity.class));
            }
        });

        // start BackgroundService for discovering nearby devices
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Intent intent = new Intent(getActivity(), BackgroundService.class);
//                //Objects.requireNonNull(getContext()).startService(intent);
//                ContextCompat.startForegroundService(Objects.requireNonNull(getActivity()), intent);
//            }
//        }).start();

        // get past 13 day sum of contacts.
        past13DaySum = findPast13DaySum();
        findAverageAndMaxRiskAndBTOffTime();

        // Assign values to the stats
        setStatsValues();

        return root;
    }

    // receiver when new device is found. update contacts today
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(action!=null && action.equals("ACTION_UPDATE_CONTACTS"))
            {
                Log.i(TAG, "receiver : updating contacts today");
                Cursor cursor = myDb.getAllData();
                if(cursor!=null)
                {
                    SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
                    contactsTodayVar = cursor.getCount() + settings.getInt("contactsTodayPenalty", 0);
                    todaysNum.setText(String.valueOf(contactsTodayVar));
                    avgNumVar = (double) (past13DaySum + contactsTodayVar) / totalDays;
                    avgContacts.setText(String.format(Locale.getDefault(),"%.2f", avgNumVar));
                    if(contactsTodayVar>maxContactsVar) {
                        maxContactsVar = contactsTodayVar;
                        maxContactsText.setText(String.valueOf(maxContactsVar));
                    }
                    cursor.close();
                }
            }
            else if(action!=null && action.equals("ACTION_UPDATE_RISK_HALF_HOUR"))
            {
                Log.i("TAB2", "Half hour receiver");
                SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
                // update BTOffTime.
                BTOffTime = settings.getFloat("totalBTOffTime", 0);
                BTOffTimeText.setText(BTOffTime+" Hrs");

                violationNumVar = settings.getInt("crowdNo", 0);
                violationNumText.setText(String.valueOf(violationNumVar));

                if(BTOffTime>maxBTOffTime) {
                    maxBTOffTime = BTOffTime;
                    maxBTOffTimeText.setText(maxBTOffTime + " Hrs");
                }
                if(totalDays>=14) {
                    avgBTOffTimeText.setText(String.format(Locale.getDefault(),"%.2f", (past13DaysBTOffSum + BTOffTime) / 14) + " Hrs");
                }
                else {
                    avgBTOffTimeText.setText(String.format(Locale.getDefault(),"%.2f", (past13DaysBTOffSum + BTOffTime) / totalDays)+" Hrs");
                }

                riskIndexVar = intent.getIntExtra("riskIndex", 0);
                updateRiskView();
            }
        }
    };


    private void updateRiskView()
    {
        riskIndexText.setText(riskIndexVar+"%");
        if(riskIndexVar<=33)
        {    currentStatus.setText("Low risk"); currentStatusVar="Low risk";    }
        else if(riskIndexVar<=66)
        {    currentStatus.setText("High risk");  currentStatusVar="High risk"; }
        else
        {    currentStatus.setText("Very high risk");  currentStatusVar="Very high risk";   }

        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
        maxRiskIndexVar = settings.getInt("maxRiskIndexVar", 0);
        maxRiskIndexText.setText(String.valueOf(maxRiskIndexVar)+"%");

        if(totalDays>14)
            avgRiskIndexText.setText(String.valueOf((past13DaysRiskSum+riskIndexVar)/14)+"%");
        else
            avgRiskIndexText.setText(String.valueOf((past13DaysRiskSum+riskIndexVar)/totalDays)+"%");
    }

    private void setStatsValues()
    {
        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
        Cursor cursor = myDb.getAllData();
        if (cursor != null)
        {
            contactsTodayVar = cursor.getCount() + settings.getInt("contactsTodayPenalty", 0);
            todaysNum.setText(String.valueOf(contactsTodayVar));
            cursor.close();
        }
        violationNumVar = settings.getInt("crowdNo", 0);
        BTOffTime = settings.getFloat("totalBTOffTime", 0);

        avgNumVar = (double) (past13DaySum + contactsTodayVar) / totalDays;
        avgContacts.setText(String.format(Locale.getDefault(),"%.2f", avgNumVar));
        if(contactsTodayVar>maxContactsVar)
            maxContactsVar = contactsTodayVar;
        maxContactsText.setText(String.valueOf(maxContactsVar));

        violationNumText.setText(String.valueOf(violationNumVar));

        updateRiskView();

        BTOffTimeText.setText(BTOffTime+" Hrs");

        maxBTOffTimeText.setText(maxBTOffTime+" Hrs");
        if(totalDays>=14) {
            avgBTOffTimeText.setText(String.format(Locale.getDefault(),"%.2f", (past13DaysBTOffSum + BTOffTime) / 14) + " Hrs");
        }
        else {
            avgBTOffTimeText.setText(String.format(Locale.getDefault(),"%.2f", (past13DaysBTOffSum + BTOffTime) / totalDays)+" Hrs");
        }

        bluetoothAdapter.setName(AppId+ MainActivity.myDeviceId+"_"+maxRiskIndexVar);
        Log.i(TAG, "Your name changed :"+bluetoothAdapter.getName());
    }

    private void findAverageAndMaxRiskAndBTOffTime()
    {
        Cursor cursor1 = myDb.getAllDataTable2();
        int s=0, mx=0, temp;
        float BTs=0, BTtemp, BTmx=0;
        while(cursor1!=null && cursor1.moveToNext())
        {
            temp = cursor1.getInt(2);
            BTtemp = cursor1.getFloat(3);
            BTs += BTtemp;
            s += temp;
            if(temp>mx)
                mx = temp;
            if(BTtemp>BTmx)
                BTmx = BTtemp;
        }
        past13DaysRiskSum = s;
        maxRiskIndexVar = mx;

        past13DaysBTOffSum = BTs;
        maxBTOffTime = BTmx;

        if(BTOffTime>BTmx)
            maxBTOffTime = BTOffTime;

        if(riskIndexVar>mx)
            maxRiskIndexVar = riskIndexVar;

        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
        SharedPreferences.Editor mapEditor = settings.edit();
        mapEditor.putInt("maxRiskIndexVar", maxRiskIndexVar);
        mapEditor.apply();
    }

    private int findPast13DaySum() {
        int s = 0;
        maxContactsVar = 0;
        Cursor cursor = myDb.getAllDataTable2();
        if (cursor == null)
            return 0;
        int  mx=0, temp;
        while (cursor.moveToNext()) {
            temp= cursor.getInt(1);
            s += temp;
            if(temp>mx)
                mx = temp;
        }
        cursor.close();
        maxContactsVar = mx;
        return s;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Objects.requireNonNull(getActivity()).unregisterReceiver(receiver);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_contactsTodayInfo:
                Cursor cursor = myDb.getAllData();
                String list ="DID      MR  Time\n";
                if(cursor!=null)
                {
                    while (cursor.moveToNext())
                    {
                        list += cursor.getString(0)+"   "+cursor.getInt(2)+"   "+cursor.getString(1)+"\n";
                    }
                }
                SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
                int penalty = settings.getInt("contactsTodayPenalty", 0);
                Utilities.showMessage(getActivity(), "Contacts", "Contacts Today shows number of people who were in your close proximity today.\n\n" +
                        "Maximum Contacts shows maximum number of people you have contacted in last 14 days.\n\npenalty = "+penalty+"\n\n"+list);
                return;
            case R.id.id_violationInfo:
                Utilities.showMessage(getActivity(), "Violation of social distancing", "Each time you are standing in the crowd with more than 7 people, you are violating the rules of social distancing.");
                return;
            case R.id.id_riskIndexInfo:
                String message = "\n\nCalculation in this case- \n";
                SharedPreferences settings1 = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
                message += "\nfromContactsRiskMax : "+settings1.getInt("fromContactsRiskMax", 0);
                message += "\nfromContactsToday : "+ settings1.getInt("fromContactsToday", 0);
                message += "\nfromBluetoothOffTime : "+ settings1.getInt("fromBluetoothOffTime", 0);
                message += "\nfromCrowdInstances : "+ settings1.getInt("fromCrowdInstances", 0);

                Utilities.showMessage(getActivity(), "Risk Factor", "Risk factor shows your daily risk of getting infection.\n It is based on 4 factors:\n" +
                        "1. Number of people contacted in a day\n" +
                        "2. Risk factor of your contacts\n" +
                        "3. For how much time your bluetooth was off.\n" +
                        "4. Number of times you were standing in the crowd\n"
                        +message);
                return;


            case R.id.id_bluetoothInfo:
//                Cursor cursor1 = myDb.getAllDataTable2();
//                String msg="Risk    BTOff\n";
//                while(cursor1!=null && cursor1.moveToNext())
//                {
//                    msg += cursor1.getInt(2)+"     "+cursor1.getFloat(3)+"\n";
//                }
                Utilities.showMessage(getActivity(), "Bluetooth On Time", "It shows for how many hours your bluetooth was on today. It will be updated every 30 minutes.");
        }
    }

    public static Dashboard newInstance(String param1, String param2) {
        Dashboard fragment = new Dashboard();
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

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }
}
