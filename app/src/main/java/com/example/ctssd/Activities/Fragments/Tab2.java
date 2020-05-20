
/* This Tab will is for displaying all stats like currents status, risk index,
   average contacts, contacts today and location.
 */

package com.example.ctssd.Activities.Fragments;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.ctssd.Activities.CoronaInfoActivity;
import com.example.ctssd.Activities.Main2Activity;
import com.example.ctssd.Activities.SelfAssessmentReport;
import com.example.ctssd.R;
import com.example.ctssd.Services.BackgroundService;
import com.example.ctssd.Utils.DatabaseHelper;
import com.example.ctssd.Utils.GetLocation;
import com.example.ctssd.Utils.Utilities;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class Tab2 extends Fragment implements View.OnClickListener
{
    private BluetoothAdapter bluetoothAdapter;
    private static final String AppId = "c1t2";
    private static final String TAG = "TAB2";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private OnFragmentInteractionListener mListener;
    private ProgressBar currentStatusPBar, riskIndexPBar, averageContactsPBar, locationStatPBar, zoneColorPBar;
    private TextView todaysNum;
    private TextView riskIndexText, currentStatus, avgContacts, locationStat, zoneColor;
    private DatabaseHelper myDb;
    private static String currentStatusVar = "null", locationVar = "null", zoneColorVar = "null";
    private static int riskIndexVar = -1, zoneVar=0, BluetoothOffTime=0, zoneColorId=0;
    private static double avgNumVar = -1.0;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private int requestPermSecCount=0;
    private static int contactsTodayVar = 0, past13DaySum=0, totalDays=1, preContactsRiskMax=0, preFromContactsToday=0, tryingForLocationCount=0;
    private static HashMap<String, Integer> messageForRiskIndex = new HashMap<>();

    public Tab2() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_tab2, container, false);

        todaysNum = root.findViewById(R.id.id_numStat);
        riskIndexText = root.findViewById(R.id.id_riskIndexStat);
        currentStatus = root.findViewById(R.id.id_currentStatus);
        avgContacts = root.findViewById(R.id.id_avgContacts);
        locationStat = root.findViewById(R.id.id_locationStat);
        currentStatusPBar = root.findViewById(R.id.id_currentStatus_progress_bar);
        riskIndexPBar = root.findViewById(R.id.riskIndex_progress_bar);
        averageContactsPBar = root.findViewById(R.id.averageContacts_progress_bar);
        zoneColor = root.findViewById(R.id.id_zoneColor);
        locationStatPBar = root.findViewById(R.id.locationStat_progress_bar);
        zoneColorPBar = root.findViewById(R.id.zoneColor_progress_bar);
        Button CoronaInfoBTN = root.findViewById(R.id.id_goTo_CoronaInfoActivity);
        Button selfAssessBTN = root.findViewById(R.id.id_selfAssessBTN);

        RelativeLayout statusInfo = root.findViewById(R.id.id_currentStatusLayout);
        RelativeLayout numbersInfo = root.findViewById(R.id.id_contactsTodayLayout);
        RelativeLayout riskIndexInfo = root.findViewById(R.id.id_riskIndexLayout);
        RelativeLayout avgContactsInfo = root.findViewById(R.id.id_averageContactsLayout);
        RelativeLayout locationAndZoneInfo = root.findViewById(R.id.id_warningLayout);

        // For displaying info about each stat
        statusInfo.setOnClickListener(this);
        numbersInfo.setOnClickListener(this);
        riskIndexInfo.setOnClickListener(this);
        avgContactsInfo.setOnClickListener(this);
        locationAndZoneInfo.setOnClickListener(this);

        myDb = new DatabaseHelper(getActivity());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // for getting lastLocation.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(getActivity()));

        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", getActivity().MODE_PRIVATE);
        totalDays = settings.getInt("totalDays", 1);  // since the app installation
        preContactsRiskMax = settings.getInt("preContactsRiskMax", 0);

        // Every 24 hours work
        Utilities utilities = new Utilities();
        if(utilities.isTwentyFourHoursOver(getActivity()))
        {
            updateRiskIndex();
            utilities.TwentyFourHoursWork(getActivity());
        }
        else
        {
            riskIndexVar = settings.getInt("myRiskIndex", 0);
            messageForRiskIndex.put("fromContactsRiskMax", settings.getInt("fromContactsRiskMax", 0));
            messageForRiskIndex.put("fromContactsToday", settings.getInt("fromContactsToday", 0));
            messageForRiskIndex.put("fromBluetoothOffTime", settings.getInt("fromBluetoothOffTime", 0));
            messageForRiskIndex.put("fromCrowdInstances", settings.getInt("fromCrowdInstances", 0));
            messageForRiskIndex.put("fromSelfAssessReport", settings.getInt("riskFromReport", 0));
        }

        // register receiver for broadcast when contact today is changed
        IntentFilter intentFilter = new IntentFilter("ACTION_UPDATE_CONTACTS_AND_RISK");
        getActivity().registerReceiver(receiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter("ACTION_UPDATE_RISK");
        getActivity().registerReceiver(receiver, intentFilter2);

        // Goto about corona information page.
        CoronaInfoBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), CoronaInfoActivity.class));
            }
        });

        selfAssessBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SelfAssessmentReport.class));
            }
        });

        // start BackgroundService for discovering nearby devices
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getActivity(), BackgroundService.class);
                Objects.requireNonNull(getContext()).startService(intent);
            }
        }).start();

        // Assign values to the stats
        setStatsValues();

        IntentFilter intentFilter3 = new IntentFilter("LOCATION_FOUND"); getActivity().registerReceiver(locationReceiver, intentFilter3);
        IntentFilter intentFilter4 = new IntentFilter("GPS_PERMISSION"); getActivity().registerReceiver(locationReceiver, intentFilter4);
        IntentFilter intentFilter5 = new IntentFilter("ENABLE_GPS"); getActivity().registerReceiver(locationReceiver, intentFilter5);
        IntentFilter intentFilter6 = new IntentFilter("GET_LOCATION_PERMISSION"); getActivity().registerReceiver(locationReceiver, intentFilter6);
        findLocation();

        return root;
    }

    private void findLocation()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                GetLocation getLocation = new GetLocation(getActivity(), fusedLocationProviderClient);
                getLocation.findLocation();
            }
        }).start();
    }

    // receiver when new device is found. update contacts today
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            Log.i(TAG, "inside locationReceiver :"+action);
            if(action==null)    return;

            switch (action)
            {
                case "LOCATION_FOUND":
                    String city = intent.getStringExtra("cityName");
                    int z = intent.getIntExtra("zoneVar", 0);
                    Log.i(TAG, "zoneVar, cityName :"+z+", "+city);
                    if(z==2)
                    {
                        zoneColorVar = "(Red zone)";
                        zoneVar = 2;
                        zoneColorId = R.color.colorAccent;
                    }
                    else if(z==1)
                    {
                        zoneColorVar = "(Orange zone)";
                        zoneVar = 1;
                        zoneColorId = R.color.colorOrange;
                    }
                    else
                    {
                        zoneColorVar = "(Green zone)";
                        zoneVar = 0;
                        zoneColorId = R.color.colorPrimary;
                    }
                    zoneColor.setText(zoneColorVar);
                    zoneColor.setTextColor(getResources().getColor(zoneColorId));
                    locationStat.setText(city);
                    locationStat.setTextColor(getResources().getColor(zoneColorId));
                    zoneColorPBar.setVisibility(View.INVISIBLE);
                    locationStatPBar.setVisibility(View.INVISIBLE);
                    break;

                case "GPS_PERMISSION":
                    Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent1, 42);
                    break;

                case "ENABLE_GPS":
                    tryingForLocationCount++;
                    if(tryingForLocationCount>=2)
                    {
                        locationStat.setText("Not found");
                        locationStatPBar.setVisibility(View.INVISIBLE);
                    }
                    enableGPS();
                    break;

                case "GET_LOCATION_PERMISSION":
                    requestPermSecCount++;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION
                        }, 44);
                    }
                    break;
            }
        }
    };

    private void enableGPS()
    {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(Objects.requireNonNull(getActivity())).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        getActivity(),
                                        LocationRequest.PRIORITY_HIGH_ACCURACY);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Thread.sleep(10000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        Log.i(TAG, "Starting thread to find location");
                                        findLocation();
                                    }
                                }).start();

                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });
    }

    private void setStatsValues()
    {
        Cursor cursor = myDb.getAllData();
        if (cursor != null) {
            contactsTodayVar = cursor.getCount();
            todaysNum.setText(String.valueOf(contactsTodayVar));
            cursor.close();
        }

        // get past 13 day sum of contacts.
        past13DaySum = findPast13DaySum();
        avgNumVar = (double) (past13DaySum + contactsTodayVar) / totalDays;
        avgContacts.setText(String.format(Locale.getDefault(),"%.2f", avgNumVar));
        averageContactsPBar.setVisibility(View.INVISIBLE);

        if (locationVar!=null && (!locationVar.equals("null"))) {
            locationStat.setText(locationVar);
            locationStatPBar.setVisibility(View.INVISIBLE);
            locationStat.setTextColor(getResources().getColor(zoneColorId));
        }
        if (!zoneColorVar.equals("null")) {
            zoneColor.setText(zoneColorVar);
            zoneColor.setTextColor(getResources().getColor(zoneColorId));
        }

        if(riskIndexVar<=33)
        {    currentStatus.setText("Low risk"); currentStatusVar="Low risk";}
        else if(riskIndexVar<=66)
        {    currentStatus.setText("High risk");  currentStatusVar="High risk";}
        else
        {    currentStatus.setText("Very high risk");  currentStatusVar="Very high risk";}
        currentStatusPBar.setVisibility(View.INVISIBLE);
        riskIndexText.setText(riskIndexVar+"%");
    }

    // receiver when new device is found. update contacts today
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            Log.i(TAG, "receiver : updating contacts today");
            String action = intent.getAction();
            if(action!=null && action.equals("ACTION_UPDATE_CONTACTS_AND_RISK"))
            {
                Cursor cursor = myDb.getAllData();
                if(cursor!=null)
                {
                    contactsTodayVar = cursor.getCount();
                    todaysNum.setText(String.valueOf(contactsTodayVar));
                    avgNumVar = (double) (past13DaySum + contactsTodayVar) / totalDays;
                    Log.i(TAG, "AVERAGE :"+(past13DaySum + contactsTodayVar)+"  "+totalDays);
                    avgContacts.setText(String.format(Locale.getDefault(),"%.2f", avgNumVar));
                    Log.i(TAG, "Average contacts :" + String.format("%.2f", avgNumVar));
                    cursor.close();
                }
            }
            else if(action!=null && action.equals("ACTION_UPDATE_RISK"))
            {
                int riskFromReport = intent.getIntExtra("riskFromReport", 0);
                SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", getActivity().MODE_PRIVATE);
                int preRiskFromReport = settings.getInt("riskFromReport", 0);
                riskIndexVar = riskIndexVar + (riskFromReport-preRiskFromReport);

                riskIndexText.setText(riskIndexVar+"%");
                if(riskIndexVar<=33)
                {    currentStatus.setText("Low risk"); currentStatusVar="Low risk";    }
                else if(riskIndexVar<=66)
                {    currentStatus.setText("High risk");  currentStatusVar="High risk"; }
                else
                {    currentStatus.setText("Very high risk");  currentStatusVar="Very high risk";   }

                // update name of device after updating riskIndex
                bluetoothAdapter.setName(AppId+Main2Activity.myPhoneNumber+"_"+riskIndexVar);
                Log.i(TAG, "Your name changed :"+AppId+Main2Activity.myPhoneNumber+"_"+riskIndexVar);

                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("myRiskIndex", riskIndexVar);
                editor.putInt("riskFromReport", riskFromReport);
                editor.apply();

                messageForRiskIndex.put("fromSelfAssessReport", riskFromReport);
            }
        }
    };

    private void updateRiskIndex()
    {
        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", getActivity().MODE_PRIVATE);
        SharedPreferences.Editor mapEditor = settings.edit();

        // 1. max risk factors of contacts (scaled on 20)
        int fromContactsRiskMax=0;
        Cursor cursor = myDb.getAllData();
        if(cursor!=null)
        {
            while (cursor.moveToNext())
            {
                if(fromContactsRiskMax<cursor.getInt(2))
                    fromContactsRiskMax = cursor.getInt(2);
            }
        }
        if(fromContactsRiskMax>preContactsRiskMax)
        {
            fromContactsRiskMax = (20*fromContactsRiskMax)/100;
            preContactsRiskMax = fromContactsRiskMax;
            SharedPreferences.Editor ed = settings.edit();
            ed.putInt("preContactsRiskMax", preContactsRiskMax);
            ed.apply();
        }
        messageForRiskIndex.put("fromContactsRisk", fromContactsRiskMax);
        mapEditor.putInt("fromContactsRiskMax", fromContactsRiskMax);

        //2. contacts today (But really yesterday)
        int fromContactsToday=0;
        if(cursor!=null)
        {
            fromContactsToday = cursor.getCount();
        }
        if(fromContactsToday>10)
        {
            if((fromContactsToday-10)/2 < 20)
                fromContactsToday = (fromContactsToday-10)/2;
            else
                fromContactsToday = 20;
        }
        messageForRiskIndex.put("fromContactsToday", fromContactsToday);
        mapEditor.putInt("fromContactsToday", fromContactsToday);

        //3. Numbers of hours user's bluetooth was off.
        Utilities utilities = new Utilities();
        BluetoothOffTime = (int) utilities.getTotalBluetoothOffTime(Objects.requireNonNull(getActivity()));
        Log.i(TAG, "TotalBTOffTime :" + BluetoothOffTime);
        int fromBluetoothOffTime = 0;
        if(BluetoothOffTime<15)
            fromBluetoothOffTime += BluetoothOffTime;
        else
            fromBluetoothOffTime += 15;
        messageForRiskIndex.put("fromBluetoothOffTime", fromBluetoothOffTime);
        mapEditor.putInt("fromBluetoothOffTime", fromBluetoothOffTime);

        //4. Number of times user was standing in crowd.
        int CrowdNo = settings.getInt("crowdNo", 0);
        int fromCrowdInstances = CrowdNo*5;
        messageForRiskIndex.put("fromCrowdInstances", fromCrowdInstances);
        mapEditor.putInt("fromCrowdInstances", fromCrowdInstances);

        // 5. from Self assessment report.
        int fromSelfAssessReport = settings.getInt("riskFromReport", 0);
        Log.i(TAG, "riskFromReport :"+fromSelfAssessReport);
        messageForRiskIndex.put("fromSelfAssessReport", fromSelfAssessReport);

        int tempRiskIndex = fromContactsRiskMax+fromBluetoothOffTime+fromContactsToday+fromCrowdInstances+fromSelfAssessReport;
        if(riskIndexVar!=tempRiskIndex)
        {
            riskIndexVar = tempRiskIndex;
            if(riskIndexVar>100)
                riskIndexVar=100;
            riskIndexText.setText(riskIndexVar + "%");
            riskIndexPBar.setVisibility(View.INVISIBLE);

            if(riskIndexVar<=33)
            {    currentStatus.setText("Low risk"); currentStatusVar="Low risk";}
            else if(riskIndexVar<=66)
            {    currentStatus.setText("High risk");  currentStatusVar="High risk";}
            else
            {    currentStatus.setText("Very high risk");  currentStatusVar="Very high risk";}

            // update name of device after updating riskIndex
            bluetoothAdapter.setName(AppId+Main2Activity.myPhoneNumber+"_"+riskIndexVar);
            Log.i(TAG, "Your name changed :"+AppId+Main2Activity.myPhoneNumber+"_"+riskIndexVar);

            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("myRiskIndex", riskIndexVar);
            editor.apply();
        }
    }

    private int findPast13DaySum() {
        int s = 0;
        Cursor cursor = myDb.getAllDataTable2();
        if (cursor == null)
            return 0;
        int i=0;
        while (cursor.moveToNext()) {
            s += cursor.getInt(1);
            i++;
        }
        Log.i(TAG, "Total records in table2 - "+ i);
        cursor.close();
        return s;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==42)
        {
            findLocation();
        }
        else if (requestCode==44)
        {
            if(grantResults[0] == PackageManager.PERMISSION_DENIED && requestPermSecCount<=2)
                findLocation();
            else
                findLocation();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i(TAG, "inside onActivityResult");
        switch (requestCode) {
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                switch (resultCode)
                {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "onActivityResult: GPS Enabled by user");
                        findLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "onActivityResult: User rejected GPS request");
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Objects.requireNonNull(getActivity()).unregisterReceiver(receiver);
            Objects.requireNonNull(getActivity()).unregisterReceiver(locationReceiver);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_contactsTodayLayout:
                Cursor cursor = myDb.getAllData();
                String list ="Phone number        Risk factor\n";
                if(cursor!=null)
                {
                    while (cursor.moveToNext())
                    {
                        list += cursor.getString(0)+"    "+cursor.getInt(2)+"\n";
                    }
                }
                Utilities.showMessage(getActivity(), "Todays Contacts", list);
                return;
            case R.id.id_averageContactsLayout:
                Utilities.showMessage(getActivity(), "Average", "Average number of people you have contacted in last 14 days.");
                return;
            case R.id.id_currentStatusLayout:
                Utilities.showMessage(getActivity(), "Current Status", "Meaning of keywords used...\n\npositive - You are Covid19 positive patient.\n\ndirect_contact - You have directly contacted covid19 positive person.\n\n" +
                        "indirect_contact - You have indirectly contacted covid19 positive person.\n\nsafe - You have not contacted any covid19 positive person directly or indirectly.");
                return;
            case R.id.id_riskIndexLayout:
                String message = "Calculation of risk index \n";
                for(HashMap.Entry element : messageForRiskIndex.entrySet())
                {
                    message += element.getKey() +" : "+ element.getValue()+"\n";
                }
                Utilities.showMessage(getActivity(), "Risk Index", message);
                // It is percentage of risk based on your contacts in last 14 days, your location and whether you have contacted positive patient directly or indirectly.
                return;

            case R.id.id_warningLayout:
                Utilities.showMessage(getActivity(), "Location status", "It shows whether your current location is red zone or green zone.");
        }
    }

    public static Tab2 newInstance(String param1, String param2) {
        Tab2 fragment = new Tab2();
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
