package com.example.ctssd.Activities.Fragments;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

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

import com.example.ctssd.Activities.CoronaInfoActivity;
import com.example.ctssd.Activities.Main2Activity;
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
import java.util.List;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;


public class Tab2 extends Fragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private OnFragmentInteractionListener mListener;
    private ProgressBar currentStatusPBar, riskIndexPBar, averageContactsPBar, locationStatPBar, zoneColorPBar;

    private static final int LIMIT = 100;
    private TextView todaysNum;
    private TextView riskIndexText, currentStatus, avgContacts, locationStat, zoneColor;
    private Button goToCornaInfoBTN;
    private DatabaseHelper myDb;
    private DatabaseReference databaseReference;
    private static String currentStatusVar = "null", locationVar = "null", zoneColorVar = "null";
    private static int riskIndexVar = -1, avgNumVar = -1;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private BluetoothAdapter bluetoothAdapter;
    private int requestBTCount=0, requestPermFirstCount = 0, requestPermSecCount = 0, requestLocEnableCount = 0;
    private static int contactsTodayVar = 0, past13DaySum=0;

    public Tab2() {
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
        goToCornaInfoBTN = root.findViewById(R.id.id_goTo_CoronaInfoActivity);
        zoneColor = root.findViewById(R.id.id_zoneColor);
        locationStatPBar = root.findViewById(R.id.locationStat_progress_bar);
        zoneColorPBar = root.findViewById(R.id.zoneColor_progress_bar);

        RelativeLayout statusInfo = root.findViewById(R.id.id_currentStatusLayout);
        RelativeLayout numbersInfo = root.findViewById(R.id.id_contactsTodayLayout);
        RelativeLayout riskIndexInfo = root.findViewById(R.id.id_riskIndexLayout);
        RelativeLayout avgContactsInfo = root.findViewById(R.id.id_averageContactsLayout);
        RelativeLayout warningLevelInfo = root.findViewById(R.id.id_warningLayout);

        statusInfo.setOnClickListener(this);
        numbersInfo.setOnClickListener(this);
        riskIndexInfo.setOnClickListener(this);
        avgContactsInfo.setOnClickListener(this);
        warningLevelInfo.setOnClickListener(this);

        Intent dIntent =  new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        dIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
        startActivityForResult(dIntent, 45);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        setStatsValues();

        myDb = new DatabaseHelper(getActivity());


        Cursor cursor = myDb.getAllData();
        if (cursor != null) {
            contactsTodayVar = cursor.getCount();
            todaysNum.setText(String.valueOf(contactsTodayVar));
            cursor.close();
        }

        IntentFilter intentFilter = new IntentFilter("ACTION_update_contacts_today");
        getActivity().registerReceiver(receiver, intentFilter);

        goToCornaInfoBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), CoronaInfoActivity.class));
            }
        });

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        databaseReference.child(Main2Activity.myMacAdd).child("status").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            Log.i("Status", "inside on data change");
                            currentStatusVar = dataSnapshot.getValue(String.class);
                            Log.i("Status", "status from database -" + currentStatusVar);
                            if(currentStatusVar==null || currentStatusVar.equals("")) {
                                currentStatus.setText("safe");
                                currentStatusVar = "safe";
                                databaseReference.child(Main2Activity.myMacAdd).child("status").setValue("safe");
                            }
                            currentStatusPBar.setVisibility(View.INVISIBLE);
                            if(!zoneColorVar.equals("null"))
                            {
                                if(zoneColorVar.equals("(Green zone)"))
                                {
                                    updateRiskIndex(0);
                                }
                                else
                                {
                                    updateRiskIndex(1);
                                }
                            }
                            else
                            {
                                updateRiskIndex(0);
                            }
                        }
                        else
                        {
                            currentStatusVar = "safe";
                            Log.i("Status", "dataSnapshot does not exists");
                            currentStatus.setText(currentStatusVar);
                            databaseReference.child(Main2Activity.myMacAdd).child("status").setValue("safe");
                        }

                        setStatsValues();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                }
        );

        past13DaySum = findPast13DaySum();

        Log.i("Average contacts", String.valueOf((double)(past13DaySum+contactsTodayVar)/14));
        avgContacts.setText(String.valueOf((past13DaySum+contactsTodayVar)/14));
        averageContactsPBar.setVisibility(View.INVISIBLE);
        return root;
    }

    private void setStatsValues()
    {
        if (!currentStatusVar.equals("null")) {
            Log.i("Status", "static currentStatusVar not null");
            currentStatus.setText(currentStatusVar);
            currentStatusPBar.setVisibility(View.INVISIBLE);
        }
        if (riskIndexVar != -1) {
            riskIndexText.setText(riskIndexVar + "%");
            riskIndexPBar.setVisibility(View.INVISIBLE);
        }
        if (avgNumVar != -1) {
            avgContacts.setText(String.valueOf(avgNumVar));
            averageContactsPBar.setVisibility(View.INVISIBLE);
        }
        if (!locationVar.equals("null")) {
            locationStat.setText(locationVar);
            locationStatPBar.setVisibility(View.INVISIBLE);
        }
        if (!zoneColorVar.equals("null")) {
            zoneColor.setText(zoneColorVar);
            zoneColor.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    private void getPermissions() {
        requestPermFirstCount++;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            }, 43);
        }
    }

    private void setupBTAndStartService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(getActivity(), "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            requestBTCount++;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 41);
        }
        else if(bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            //bluetoothAdapter.setName(Main2Activity.myMacAdd);
            Log.i("tab2", "background service started");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(getActivity(), BackgroundService.class);
                    getContext().startService(intent);
                }
            }).start();

            getLocation();
        }
    }

    // receiver when new device is found. update contacts today
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            Log.i("Tab2 Receiver", "updating contacts today");
            Cursor cursor = myDb.getAllData();
            if(cursor!=null) {
                contactsTodayVar = cursor.getCount();
                todaysNum.setText(String.valueOf(contactsTodayVar));
                avgContacts.setText(String.valueOf((past13DaySum+contactsTodayVar)/14));
                Log.i("Average contacts", String.valueOf((double)(past13DaySum+contactsTodayVar)/14.0));
                cursor.close();
            }
        }
    };

    private void updateRiskIndex(int zone) {
        Log.i( "status",String.valueOf(getStatusInt(currentStatusVar)));
        riskIndexVar = ((avgNumVar / 3) + getStatusInt(currentStatusVar)) + 30*zone;
        if(riskIndexVar>100)
            riskIndexVar=100;

        riskIndexText.setText(riskIndexVar + "%");
        riskIndexPBar.setVisibility(View.INVISIBLE);
    }

    private int getStatusInt(String currentStatusVar) {
        if (currentStatusVar.equals("positive")) {
            return 100;
        } else if (currentStatusVar.equals("direct_contact")) {
            return 50;
        } else if (currentStatusVar.equals("indirect_contact")) {
            return 25;
        } else
            return 0;
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

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_contactsTodayLayout:
                Utilities.showMessage(getActivity(), "Todays Contacts", "Number of people that were in your range today.");
                return;
            case R.id.id_averageContactsLayout:
                Utilities.showMessage(getActivity(), "Average", "Average number of people you have contacted in last 14 days.");
                return;
            case R.id.id_currentStatusLayout:
                Utilities.showMessage(getActivity(), "Current Status", "Meaning of keywords used...\n\npositive - You are Covid19 positive patient.\n\ndirect_contact - You have directly contacted covid19 positive person.\n\n" +
                        "indirect_contact - You have indirectly contacted covid19 positive person.\n\nsafe - You have not contacted any covid19 positive person directly or indirectly.");
                return;
            case R.id.id_riskIndexLayout:
                Utilities.showMessage(getActivity(), "Risk Index", "It is percentage of risk based on your contacts in last 14 days, your location and whether you have contacted positive patient directly or indirectly.");
                return;

            case R.id.id_warningLayout:
                Utilities.showMessage(getActivity(), "Location status", "It shows whether your current location is red zone or green zone.");
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    private void getLocation() {

        Log.i("Location", "inside get location");
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
            Log.i("Loc", "getting permission");
        } else {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            //mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
            mLocationRequest.setInterval(0);
            mLocationRequest.setFastestInterval(0);
            mLocationRequest.setNumUpdates(1);
            Log.i("Loc", "looper");
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
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
                }
                return;
            }
            locationStat.setText(locationVar);
            locationStatPBar.setVisibility(View.INVISIBLE);
            zoneColor.setVisibility(View.VISIBLE);
            final String area = addresses.get(0).getAdminArea();

            // check if any of this is in hotspot list
            DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference().child("hotspots");
            dataRef.child("list").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    if(dataSnapshot.exists())
                    {
                         String list = dataSnapshot.getValue(String.class);
                         if(list!=null && cityName!=null && (list.contains(cityName) || list.contains(area)))
                         {
                             zoneColorVar = "(Red zone)";
                             zoneColor.setText(zoneColorVar);
                             zoneColor.setTextColor(Color.RED);
                             locationStat.setTextColor(Color.RED);
                             zoneColorPBar.setVisibility(View.INVISIBLE);
                             if(!currentStatusVar.equals("null"))
                                 updateRiskIndex(1);
                         }
                         else
                         {
                             zoneColorVar = "(Green zone)";
                             zoneColor.setText(zoneColorVar);
                             zoneColor.setTextColor(getResources().getColor(R.color.colorPrimary));
                             zoneColorPBar.setVisibility(View.INVISIBLE);
                             if(!currentStatusVar.equals("null"))
                                 updateRiskIndex(0);
                         }
                    }
                    else
                    {
                        Log.i("Database", "dataSnapshot not exists");
                        zoneColorVar = "(Green zone)";
                        zoneColor.setText(zoneColorVar);
                        zoneColor.setTextColor(getResources().getColor(R.color.colorPrimary));
                        zoneColorPBar.setVisibility(View.INVISIBLE);
                        if(!currentStatusVar.equals("null"))
                            updateRiskIndex(0);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isLocationEnabled_Network() {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    private boolean isLocationEnabled_GPS() {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case 43:
                if (grantResults.length > 2 && grantResults[2] == PackageManager.PERMISSION_GRANTED)
                {
                    // got the location permission.
                    setupBTAndStartService();
                    //getLocation();
                }
                else
                {
                    if(requestPermFirstCount<=2)
                        getPermissions();
                }
                break;

            case 44:
                if (requestPermSecCount<=2)
                    getLocation();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case 41:
                // check if bluetooth is on.
                if(bluetoothAdapter.isEnabled())
                {
                    setupBTAndStartService();
                }
                else if(!bluetoothAdapter.isEnabled() && requestBTCount<=2){
                    Toast.makeText(getActivity(), "Please turn on bluetooth", Toast.LENGTH_LONG).show();
                    setupBTAndStartService();
                }
                break;
            case 42:
                Log.i("Location", "Got the permission maybe");
                if(requestLocEnableCount<=2 || isLocationEnabled_GPS())
                    requestNewLocationData();
                break;

            case 45:
                setStatsValues();
                getPermissions();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            getActivity().unregisterReceiver(receiver);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
