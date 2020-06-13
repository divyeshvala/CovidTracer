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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class Tab2 extends Fragment implements View.OnClickListener {
    private BluetoothAdapter bluetoothAdapter;
    private static final String AppId = "c1t2";
    private static final String TAG = "TAB2";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private OnFragmentInteractionListener mListener;
    //private ProgressBar locationStatPBar;
    private TextView todaysNum;
    private TextView riskIndexText, currentStatus, avgContacts, violationNumText,
            maxContactsText, maxRiskIndexText, avgRiskIndexText, BTonTimeText,
            maxBTonTimeText, avgBTonTimeText;
    //private TextView zoneColor, locationStat;
    private DatabaseHelper myDb;
    private static String currentStatusVar = "null", locationVar = "null", zoneColorVar = "null";
    private static int riskIndexVar = -1, past13DaysRiskSum = 0, maxRiskIndexVar = 0, maxContactsVar = 0,
            zoneVar = 0, BluetoothOffTime = 0, zoneColorId = 0;
    private static float BTonTime = 0, maxBTonTime = 0, past13DaysBTonSum = 0;

    private static double avgNumVar = 0.0;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private int requestPermSecCount = 0;
    private static int contactsTodayVar = 0, past13DaySum = 0, totalDays = 1,
            tryingForLocationCount = 0, violationNumVar = 0;
    private static boolean isRiskUpToDate = false;

    public Tab2() {
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            String coordinates = location.getLatitude() + "_" + location.getLongitude();
            Log.i("Location", "coordinates found :" + coordinates);
            Intent intent1 = new Intent("COORDINATES_FOUND");
            intent1.putExtra("coordinates", coordinates);
            intent1.putExtra("latitude", location.getLatitude());
            intent1.putExtra("longitude", location.getLongitude());
            Objects.requireNonNull(getActivity()).sendBroadcast(intent1);
            Toast.makeText(getActivity(), coordinates, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i("LocationTab2", "Provider disabled");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i("LocationTab2", "Provider disabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
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
        //locationStat = root.findViewById(R.id.id_locationStat);
        //zoneColor = root.findViewById(R.id.id_zoneColor);
        //locationStatPBar = root.findViewById(R.id.locationStat_progress_bar);
        violationNumText = root.findViewById(R.id.id_violationText);
        BTonTimeText = root.findViewById(R.id.id_bluetoothText);
        maxBTonTimeText = root.findViewById(R.id.id_maxBluetoothText);
        avgBTonTimeText = root.findViewById(R.id.id_avgBluetooth);
        RelativeLayout CoronaInfoBTN = root.findViewById(R.id.id_goTo_CoronaInfoActivity);

        //ImageView statusInfo = root.findViewById(R.id.id_currentStatusInfo);
        ImageView numbersInfo = root.findViewById(R.id.id_contactsTodayInfo);
        ImageView riskIndexInfo = root.findViewById(R.id.id_riskIndexInfo);
        //ImageView locationAndZoneInfo = root.findViewById(R.id.id_locationInfo);
        ImageView violationInfo = root.findViewById(R.id.id_violationInfo);
        ImageView bluetoothInfo = root.findViewById(R.id.id_bluetoothInfo);

        numbersInfo.setOnClickListener(this);
        riskIndexInfo.setOnClickListener(this);
        violationInfo.setOnClickListener(this);
        bluetoothInfo.setOnClickListener(this);

        myDb = new DatabaseHelper(getActivity());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // for getting lastLocation.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(getActivity()));

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
        BTonTime = settings.getFloat("totalBTOnTime", 0);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getActivity(), BackgroundService.class);
                //Objects.requireNonNull(getContext()).startService(intent);
                ContextCompat.startForegroundService(Objects.requireNonNull(getActivity()), intent);
            }
        }).start();

        // get past 13 day sum of contacts.
        past13DaySum = findPast13DaySum();
        findAverageAndMaxRiskAndBTonTime();

        // Assign values to the stats
        setStatsValues();

        // Getting location of user.
        //IntentFilter intentFilter3 = new IntentFilter("LOCATION_FOUND"); getActivity().registerReceiver(locationReceiver, intentFilter3);
        IntentFilter intentFilter4 = new IntentFilter("GPS_PERMISSION");
        getActivity().registerReceiver(locationReceiver, intentFilter4);
        IntentFilter intentFilter5 = new IntentFilter("ENABLE_GPS");
        getActivity().registerReceiver(locationReceiver, intentFilter5);
        IntentFilter intentFilter6 = new IntentFilter("GET_LOCATION_PERMISSION");
        getActivity().registerReceiver(locationReceiver, intentFilter6);
        // findLocation();

        // getActivity().startService(new Intent(getActivity(), LocationService.class));

        // mService.requestLocationUpdates(getActivity());

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Intent intent = new Intent(getActivity(), BackgroundLocationService.class);
//                //Objects.requireNonNull(getContext()).startService(intent);
//                ContextCompat.startForegroundService(Objects.requireNonNull(getActivity()), intent);
//            }
//        }).start();

        //BackgroundService backgroundService = new BackgroundService();
        //backgroundService.setupLocation(getActivity());

//        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
//        LocationListener locationListener = new MyLocationListener();
//
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//            Log.i("LocationTab2", "requestLocUpdates1.1");
//            if (ActivityCompat.checkSelfPermission(getActivity(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(),Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                Log.i("LocationTab2", "requestLocUpdates1.2");
//                locationManager.requestLocationUpdates(
//                        LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
//            }
//        }
//        else
//        {
//            Log.i("LocationTab2", "requestLocUpdates2");
//            locationManager.requestLocationUpdates(
//                    LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
//        }
//
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//            Log.i("LocationTab21", "requestLocUpdates1.1");
//            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                Log.i("LocationTab21", "requestLocUpdates1.2");
//                locationManager.requestLocationUpdates(
//                        LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
//            }
//        }
//        else
//        {
//            Log.i("LocationTab21", "requestLocUpdates2");
//            locationManager.requestLocationUpdates(
//                    LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
//        }

        return root;
    }

    private void findLocation()
    {
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                GetLocation getLocation = new GetLocation(getActivity(), fusedLocationProviderClient);
                getLocation.findLocation();
            }
        }).start();
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
//            else if(action!=null && action.equals("ACTION_UPDATE_RISK"))
//            {
//                int riskFromReport = intent.getIntExtra("riskFromReport", 0);
//                // scale it on 20.
//                riskFromReport = (20*riskFromReport)/27;
//
//                SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
//                int preRiskFromReport = settings.getInt("riskFromReport", 0);
//                riskIndexVar = riskIndexVar + (riskFromReport-preRiskFromReport);
//
//                SharedPreferences.Editor editor = settings.edit();
//                editor.putInt("myRiskIndex", riskIndexVar);
//                editor.putInt("riskFromReport", riskFromReport);
//                editor.putBoolean("isAlreadySubmitted", true);
//                editor.putString("lastSumbit", Calendar.getInstance().get(Calendar.DAY_OF_MONTH)+"-"+Calendar.getInstance().get(Calendar.MONTH)+"-"+Calendar.getInstance().get(Calendar.YEAR));
//                editor.apply();
//
//                updateRiskView();
//
//                Intent intent2 = new Intent("ACTION_UPDATE_RISK_GRAPH");
//                intent2.putExtra("riskIndex", riskIndexVar);
//                getActivity().sendBroadcast(intent2);
//            }
            else if(action!=null && action.equals("ACTION_UPDATE_RISK_HALF_HOUR"))
            {
                Log.i("TAB2", "Half hour receiver");
                SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
                // update BTonTime.
                BTonTime = settings.getFloat("totalBTOnTime", 0);
                BTonTimeText.setText(BTonTime+" Hrs");

                violationNumVar = settings.getInt("crowdNo", 0);
                violationNumText.setText(String.valueOf(violationNumVar));

                if(BTonTime>maxBTonTime) {
                    maxBTonTime = BTonTime;
                    maxBTonTimeText.setText(maxBTonTime + " Hrs");
                }
                if(totalDays>=14) {
                    avgBTonTimeText.setText(String.format(Locale.getDefault(),"%.2f", (past13DaysBTonSum + BTonTime) / 14) + " Hrs");
                }
                else {
                    avgBTonTimeText.setText(String.format(Locale.getDefault(),"%.2f", (past13DaysBTonSum + BTonTime) / totalDays)+" Hrs");
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
        BTonTime = settings.getFloat("totalBTOnTime", 0);

        avgNumVar = (double) (past13DaySum + contactsTodayVar) / totalDays;
        avgContacts.setText(String.format(Locale.getDefault(),"%.2f", avgNumVar));
        if(contactsTodayVar>maxContactsVar)
            maxContactsVar = contactsTodayVar;
        maxContactsText.setText(String.valueOf(maxContactsVar));

        violationNumText.setText(String.valueOf(violationNumVar));

//        if (locationVar!=null && (!locationVar.equals("null"))) {
//            locationStat.setText(locationVar);
//            locationStatPBar.setVisibility(View.INVISIBLE);
//            locationStat.setTextColor(getResources().getColor(zoneColorId));
//        }
//        if (!zoneColorVar.equals("null")) {
//            zoneColor.setText(zoneColorVar);
//            zoneColor.setTextColor(getResources().getColor(zoneColorId));
//        }

        updateRiskView();

        BTonTimeText.setText(BTonTime+" Hrs");

        maxBTonTimeText.setText(maxBTonTime+" Hrs");
        if(totalDays>=14) {
            avgBTonTimeText.setText(String.format(Locale.getDefault(),"%.2f", (past13DaysBTonSum + BTonTime) / 14) + " Hrs");
        }
        else {
            avgBTonTimeText.setText(String.format(Locale.getDefault(),"%.2f", (past13DaysBTonSum + BTonTime) / totalDays)+" Hrs");
        }

        bluetoothAdapter.setName(AppId+Main2Activity.myPhoneNumber+"_"+maxRiskIndexVar);
        Log.i(TAG, "Your name changed :"+bluetoothAdapter.getName());
    }

    private void findAverageAndMaxRiskAndBTonTime()
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

        past13DaysBTonSum = BTs;
        maxBTonTime = BTmx;

        if(BTonTime>BTmx)
            maxBTonTime = BTonTime;

        if(riskIndexVar>mx)
            maxRiskIndexVar = riskIndexVar;

        //Log.i(TAG, "risk sum, days :"+past13DaysRiskSum+", "+totalDays);
        //Log.i(TAG, "BT sum, days :"+past13DaysBTonSum+", "+totalDays);

        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", MODE_PRIVATE);
        SharedPreferences.Editor mapEditor = settings.edit();
        mapEditor.putInt("maxRiskIndexVar", maxRiskIndexVar);
        mapEditor.apply();
    }

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
//                    zoneColor.setText(zoneColorVar);
//                    zoneColor.setTextColor(getResources().getColor(zoneColorId));
//                    locationStat.setText(city);
//                    locationStat.setTextColor(getResources().getColor(zoneColorId));
//                    locationStatPBar.setVisibility(View.INVISIBLE);
                    break;

                case "GPS_PERMISSION":
                    Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent1, 42);
                    break;

                case "ENABLE_GPS":
                    tryingForLocationCount++;
                    if(tryingForLocationCount>=2)
                    {
                        //locationStat.setText("Not found");
                        //locationStatPBar.setVisibility(View.INVISIBLE);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
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

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//
//        Log.i(TAG, "inside onActivityResult");
//        switch (requestCode)
//        {
//            case LocationRequest.PRIORITY_HIGH_ACCURACY:
//                switch (resultCode)
//                {
//                    case Activity.RESULT_OK:
//                        Log.i(TAG, "onActivityResult: GPS Enabled by user");
//                        findLocation();
//                        break;
//                    case Activity.RESULT_CANCELED:
//                        Log.i(TAG, "onActivityResult: User rejected GPS request");
//                        break;
//                    default:
//                        break;
//                }
//                break;
//        }
//    }

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

//            case R.id.id_locationInfo:
//                Utilities.showMessage(getActivity(), "Location status", "It shows whether your current location is red zone or green zone.\n");
//                return;
            case R.id.id_bluetoothInfo:
//                Cursor cursor1 = myDb.getAllDataTable2();
//                String msg="Risk    BTon\n";
//                while(cursor1!=null && cursor1.moveToNext())
//                {
//                    msg += cursor1.getInt(2)+"     "+cursor1.getFloat(3)+"\n";
//                }
                Utilities.showMessage(getActivity(), "Bluetooth On Time", "It shows for how many hours your bluetooth was on today. It will be updated every 30 minutes.");
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
