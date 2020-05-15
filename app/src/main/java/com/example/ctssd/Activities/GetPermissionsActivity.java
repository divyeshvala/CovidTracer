package com.example.ctssd.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.ctssd.R;

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
        myPhoneNumber = settings.getString("myPhoneNumber", "NA");
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
                Intent intent = new Intent(GetPermissionsActivity.this, Main2Activity.class);
                startActivity(intent);
                finish();
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
            }, 43);
        }
    }
}
