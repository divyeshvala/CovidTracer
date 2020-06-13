package com.example.ctssd.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.ctssd.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Objects;

public class GetPermissionsActivity extends AppCompatActivity {

    private static final String TAG = "GetPermissionsActivity";
    private BluetoothAdapter bluetoothAdapter;
    private static final String AppId = "c1t2";
    private int requestBTCount=0, requestPermFirstCount = 0;
    private int riskIndex;
    private String myPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_permissions);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        myPhoneNumber = settings.getString("myDeviceId", "NA");
        riskIndex = settings.getInt("myRiskIndex", 0);
        Log.i(TAG, "RiskIndex from shared preferences :"+riskIndex);

        /* from here flow will be as follows
         * 1. call getPermissions()
         * 2. Go to onRequestPermissionResult and call setupBluetooth.
         * 3. inside setupBluetooth request user to make device discoverable.
         * 4. Go to onActivityResult and create intent switch activity.
         */

        getPermissions();

    }

    private void setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.i(TAG, "Inside setup bluetooth");
        if (bluetoothAdapter == null) {
            Toast.makeText(GetPermissionsActivity.this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            requestBTCount++;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 41);
        }
        else if(bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            bluetoothAdapter.setName(AppId+myPhoneNumber+"_"+riskIndex);
            Log.i(TAG, "Your device name :"+AppId+myPhoneNumber+"_"+riskIndex);

            // For making device discoverable.
            Intent dIntent =  new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            dIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
            startActivityForResult(dIntent, 45);
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
                    setupBluetooth();
                }
                else if(!bluetoothAdapter.isEnabled() && requestBTCount<=2)
                {
                    Toast.makeText(GetPermissionsActivity.this, "Please turn on bluetooth", Toast.LENGTH_LONG).show();
                    setupBluetooth();
                }
                break;

            case 45:
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    enableGPS();
                else
                {
                    Intent intent = new Intent(GetPermissionsActivity.this, Main2Activity.class);
                    startActivity(intent);
                    finish();
                }
                break;

            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                switch (resultCode)
                {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "Got it : GPS Enabled by user");
                        Intent intent = new Intent(GetPermissionsActivity.this, Main2Activity.class);
                        startActivity(intent);
                        finish();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "Cancelled : User rejected GPS request");
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case 43:
                if (grantResults.length > 2 && grantResults[2] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.i(TAG, "Permission 1 granted");
                    setupBluetooth();
                }
                else if(requestPermFirstCount<=2) {
                    getPermissions();
                }
                break;
        }
    }

    private void getPermissions()
    {
        requestPermFirstCount++;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 43);
        }
    }

    private void enableGPS()
    {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

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
                                        GetPermissionsActivity.this,
                                        LocationRequest.PRIORITY_HIGH_ACCURACY);

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
}
