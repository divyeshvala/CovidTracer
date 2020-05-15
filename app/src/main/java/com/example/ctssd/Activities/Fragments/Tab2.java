
/* This Tab will is for displaying all stats like currents status, risk index,
   average contacts, contacts today and location.
 */

package com.example.ctssd.Activities.Fragments;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.ctssd.Activities.CoronaInfoActivity;
import com.example.ctssd.Activities.Main2Activity;
import com.example.ctssd.Activities.SelfAssessmentReport;
import com.example.ctssd.R;
import com.example.ctssd.Services.BackgroundService;
import com.example.ctssd.Utils.DatabaseHelper;
import com.example.ctssd.Utils.Utilities;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.content.Context.LOCATION_SERVICE;

public class Tab2 extends Fragment implements View.OnClickListener
{
    private BluetoothAdapter bluetoothAdapter;
    private static final String AppId = "c1t2";
    private static final int CROWD_INSTANCES_LIMIT = 1;  // TODO: change it later
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
    private int requestLocEnableCount = 0, requestPermSecCount=0;
    private static int contactsTodayVar = 0, past13DaySum=0, totalDays=1, preContactsRiskMax=0, preFromContactsToday=0;
    private static HashMap<String, Integer> messageForRiskIndex = new HashMap<>();
    private static long preCrowdTime = -1;

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

        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", getActivity().MODE_PRIVATE);
        riskIndexVar = settings.getInt("myRiskIndex", 0);
        totalDays = settings.getInt("totalDays", 1);  // since the app installation
        preContactsRiskMax = settings.getInt("preContactsRiskMax", 0);
        preFromContactsToday = settings.getInt("preFromContactsToday", 0);

        Log.i(TAG, "Initial - myRiskIndex, riskFromReport :"+riskIndexVar+"  "+settings.getInt("riskFromReport", 0));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        myDb = new DatabaseHelper(getActivity());

        // for getting lastLocation.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(getActivity()));

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

        getLocation();

        // get past 13 day sum of contacts.
        past13DaySum = findPast13DaySum();

        Log.i("Average contacts", String.valueOf((double)(past13DaySum+contactsTodayVar)/14));
        avgContacts.setText(String.valueOf((past13DaySum+contactsTodayVar)/14));
        averageContactsPBar.setVisibility(View.INVISIBLE);
        return root;
    }

    private void setStatsValues()
    {
        if (avgNumVar != -1) {
            avgContacts.setText(String.valueOf(avgNumVar));
            averageContactsPBar.setVisibility(View.INVISIBLE);
        }
        if (locationVar!=null && (!locationVar.equals("null"))) {
            locationStat.setText(locationVar);
            locationStatPBar.setVisibility(View.INVISIBLE);
            locationStat.setTextColor(getResources().getColor(zoneColorId));
        }
        if (!zoneColorVar.equals("null")) {
            zoneColor.setText(zoneColorVar);
            zoneColor.setTextColor(getResources().getColor(zoneColorId));
        }
        Cursor cursor = myDb.getAllData();
        if (cursor != null) {
            contactsTodayVar = cursor.getCount();
            todaysNum.setText(String.valueOf(contactsTodayVar));
            cursor.close();
        }
        if(riskIndexVar<=33)
        {    currentStatus.setText("Low risk"); currentStatusVar="Low risk";}
        else if(riskIndexVar<=66)
        {    currentStatus.setText("High risk");  currentStatusVar="High risk";}
        else
        {    currentStatus.setText("Very high risk");  currentStatusVar="Very high risk";}
        currentStatusPBar.setVisibility(View.INVISIBLE);
        riskIndexText.setText(riskIndexVar+"%");
        updateRiskIndex();
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

                    updateRiskIndex();
                }
            }
            else if(action!=null && action.equals("ACTION_UPDATE_RISK"))
            {
                int riskFromReport = intent.getIntExtra("riskFromReport", 0);
                Log.i(TAG, "riskFromReport from broadcast :"+riskFromReport);
                SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", getActivity().MODE_PRIVATE);
                int preRiskFromReport = settings.getInt("riskFromReport", 0);
                riskIndexVar = riskIndexVar + (riskFromReport-preRiskFromReport);
                Log.i(TAG, "preRiskFromReport, riskIndexVar :"+preRiskFromReport+", "+riskIndexVar);

                riskIndexText.setText(riskIndexVar+"%");
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
                editor.putInt("riskFromReport", riskFromReport);
                editor.apply();

                messageForRiskIndex.put("fromSelfAssessReport", riskFromReport);
            }
        }
    };

    private void updateRiskIndex()
    {
        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", getActivity().MODE_PRIVATE);
        riskIndexPBar.setVisibility(View.VISIBLE);

        // 1. max risk factors of contacts
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
            if(fromContactsRiskMax>20)
                fromContactsRiskMax=20;
            preContactsRiskMax = fromContactsRiskMax;
            SharedPreferences.Editor ed = settings.edit();
            ed.putInt("preContactsRiskMax", preContactsRiskMax);
            ed.apply();
        }
        messageForRiskIndex.put("fromContactsRiskAverage", fromContactsRiskMax);

        //2. contacts today
        int fromContactsToday=0;
        if(contactsTodayVar>10)
        {
            if((contactsTodayVar-10)/2 < 20)
                fromContactsToday = (contactsTodayVar-10)/2;
            else
                fromContactsToday = 20;
        }
        SharedPreferences.Editor edi = settings.edit();
        edi.putInt("todaysFromContactsToday", fromContactsToday);
        edi.apply();
        fromContactsToday += preFromContactsToday;
        messageForRiskIndex.put("fromContactsToday", fromContactsToday);

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

        // 4. From location
        int fromLocation = zoneVar*15;
        messageForRiskIndex.put("fromLocation", fromLocation);

        //5. Number of times user was standing in crowd.
        int crowdInstances = settings.getInt("crowdInstances", 0);
        int preCrowdNo = settings.getInt("crowdNo", 0);
        int fromCrowdInstances = preCrowdNo*5;
        if( crowdInstances >= CROWD_INSTANCES_LIMIT && ( preCrowdTime==-1 || Calendar.getInstance().getTime().getTime()-preCrowdTime>600000))
        {
            fromCrowdInstances += 5;
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("crowdInstances", 0);
            editor.putInt("crowdNo", (preCrowdNo+1));
            editor.apply();
            preCrowdTime = Calendar.getInstance().getTime().getTime();
        }
        messageForRiskIndex.put("fromCrowdInstances", fromCrowdInstances);

        // 6. from Self assessment report.
        int fromSelfAssessReport = settings.getInt("riskFromReport", 0);
        Log.i(TAG, "riskFromReport :"+fromSelfAssessReport);
        messageForRiskIndex.put("fromSelfAssessReport", fromSelfAssessReport);

        int tempRiskIndex = fromContactsRiskMax+fromBluetoothOffTime+fromContactsToday+fromCrowdInstances+fromLocation+fromSelfAssessReport;
        if(riskIndexVar!=tempRiskIndex)
        {
            riskIndexVar = tempRiskIndex;
            if(riskIndexVar>100)
                riskIndexVar=100;
            riskIndexText.setText(riskIndexVar + "%");
            riskIndexPBar.setVisibility(View.INVISIBLE);
            // TODO: change it later
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
        riskIndexPBar.setVisibility(View.INVISIBLE);
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
        Log.i("Tab2", "Total records in table2 - "+ i);
        cursor.close();
        return s;
    }

    private void getLocation()
    {
        Log.i("Location", "inside get location");
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i("Location", "permission is there");
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    Log.i("Location", "inside listener");
                    if (location != null) {
                        Log.i("Location", "location is not null");
                        getAddress(location);
                    } else {
                        Log.i("Loc", "location is null");
                        requestNewLocationData();
                    }
                }
            });
        } else {
            Log.i("Location", "permission is not there");
            requestPermSecCount++;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 44);
            }
        }
    }

    private void requestNewLocationData()
    {
        if (!isLocationEnabled_Network() && !isLocationEnabled_GPS()) {   // if both are disabled
            requestLocEnableCount++;
            Toast.makeText(getActivity(), "Please turn on location", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 42);
            Log.i(TAG, "getting permission");
        } else {
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            //mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
            mLocationRequest.setInterval(0);
            mLocationRequest.setFastestInterval(0);
            mLocationRequest.setNumUpdates(1);
            Log.i("Loc", "looper");
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(getActivity()));
            fusedLocationProviderClient.requestLocationUpdates(
                    mLocationRequest, mLocationCallback,
                    Looper.myLooper()
            );
        }
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Log.i("Loc", "onLocResult");
            Location mLastLocation = locationResult.getLastLocation();
            if (mLastLocation!=null)
            {
                getAddress(mLastLocation);
            }
            else
            {
                requestNewLocationData();
            }
        }
    };

    private void getAddress(Location location)
    {
        try {
            Geocoder geocoder = new Geocoder(getActivity(),
                    Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(),
                    1
            );
            locationVar = addresses.get(0).getLocality();
            final String cityName = addresses.get(0).getLocality();

            if(cityName==null)
            {
                if(!isLocationEnabled_GPS())
                {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, 42);
                }
                else
                {
                    locationStat.setText("couldn't find location");
                    fusedLocationProviderClient.flushLocations();
                    requestNewLocationData();
                }
                return;
            }
            locationStat.setText(locationVar);
            locationStatPBar.setVisibility(View.INVISIBLE);
            zoneColor.setVisibility(View.VISIBLE);
            final String area = addresses.get(0).getAdminArea();

            // check if any of this is in hotspot list
            DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference().child("hotspots");
            dataRef.child("lists").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    if(dataSnapshot.exists())
                    {
                         String red = dataSnapshot.child("red").getValue(String.class);
                         String orange = dataSnapshot.child("orange").getValue(String.class);
                         if(red!=null && (red.contains(cityName) || red.contains(area)))
                         {
                             zoneColorVar = "(Red zone)";
                             zoneVar = 2;
                             zoneColorId = R.color.colorAccent;
                             zoneColor.setText(zoneColorVar);
                             zoneColor.setTextColor(getResources().getColor(zoneColorId));
                             locationStat.setTextColor(getResources().getColor(zoneColorId));
                             zoneColorPBar.setVisibility(View.INVISIBLE);
                             updateRiskIndex();
                         }
                         else if(orange!=null && (orange.contains(cityName) || orange.contains(area)))
                         {
                             zoneColorVar = "(Orange zone)";
                             zoneVar = 1;
                             zoneColorId = R.color.colorOrange;
                             zoneColor.setText(zoneColorVar);
                             zoneColor.setTextColor(getResources().getColor(zoneColorId));
                             locationStat.setTextColor(getResources().getColor(zoneColorId));
                             zoneColorPBar.setVisibility(View.INVISIBLE);
                             updateRiskIndex();
                         }
                         else
                         {
                             zoneColorVar = "(Green zone)";
                             zoneVar = 0;
                             zoneColorId = R.color.colorPrimary;
                             zoneColor.setText(zoneColorVar);
                             zoneColor.setTextColor(getResources().getColor(zoneColorId));
                             locationStat.setTextColor(getResources().getColor(zoneColorId));
                             zoneColorPBar.setVisibility(View.INVISIBLE);
                             updateRiskIndex();
                         }
                    }
                    else
                    {
                        Log.i("Database", "dataSnapshot not exists");
                        zoneColorVar = "(Green zone)";
                        zoneColorId = R.color.colorPrimary;
                        zoneColor.setText(zoneColorVar);
                        zoneColor.setTextColor(getResources().getColor(zoneColorId));
                        locationStat.setTextColor(getResources().getColor(zoneColorId));
                        zoneColorPBar.setVisibility(View.INVISIBLE);
                        updateRiskIndex();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private boolean isLocationEnabled_Network() {
        LocationManager locationManager = (LocationManager) Objects.requireNonNull(getContext()).getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    private boolean isLocationEnabled_GPS() {
        LocationManager locationManager = (LocationManager) Objects.requireNonNull(getContext()).getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==42)
        {
            Log.i("Location", "Got the permission maybe");
            if(requestLocEnableCount<=2 || isLocationEnabled_GPS())
                requestNewLocationData();
        }
        else if (requestCode==44)
        {
            if(grantResults[0] == PackageManager.PERMISSION_DENIED && requestPermSecCount<=2)
                getLocation();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
