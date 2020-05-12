
/* This background service will discover all devices
 *  and add them to local storage.
 */

package com.example.ctssd.Services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import com.example.ctssd.Utils.DatabaseHelper;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Objects;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

public class BackgroundService extends Service
{
    private static final String TAG = "BackgroundService";
    private static final String AppId = "t1R2a3C2e1r";
    private DatabaseHelper myDb;
    private BluetoothAdapter bluetoothAdapter;

    public static Date startTime;
    public static float prevBTOnTime;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        myDb = new DatabaseHelper(getApplicationContext());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        startTime = new Date();
        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        prevBTOnTime = settings.getFloat("bluetoothTime", 0);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        // Register for broadcasts when discovery is finished so that we can start it again.
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter2);

        bluetoothAdapter.startDiscovery();

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                // after some time discovery gets stop automatically to save battery. So we added while loop.
//                while(true){
//                    // start discovery for devices
//                    Log.i(TAG, "discovery started");
//                    try {
//                        Thread.sleep(30000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    // send broadcast to update device list.
//
//                    // for updating list in Tab1.
////                    Intent intent = new Intent("ACTION_update_list");
////                    getApplication().sendBroadcast(intent);
//                }
//            }
//        }).start();
        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // updating total time the bluetooth is on.
        long elevenPM = 82800000;
        long sixAM = 21600000;
        Date endTime = new Date();

        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        float currentBluetoothTime;
        if(startTime.getTime()<sixAM)
        {
            currentBluetoothTime = (endTime.getTime()-sixAM)/3600000;
            editor.putFloat("bluetoothTime", prevBTOnTime + (currentBluetoothTime));
        }
        else if(endTime.getTime()>elevenPM)
        {
            currentBluetoothTime = (int) ( elevenPM-startTime.getTime())/3600000;
            editor.putFloat("bluetoothTime", prevBTOnTime + (currentBluetoothTime));
        }
        else
        {
            currentBluetoothTime = (int) ( endTime.getTime()-startTime.getTime())/3600000;
            editor.putFloat("bluetoothTime", prevBTOnTime + (currentBluetoothTime));
        }
        editor.apply();
        startTime = new Date();

        Log.i(TAG, "CurrentBTtime :"+currentBluetoothTime);

        try {
            // don't forget to unregister receiver
            unregisterReceiver(receiver);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Calendar calendar = Calendar.getInstance();
                String deviceName = device.getName();
                String time = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);

                short rssi = Objects.requireNonNull(intent.getExtras()).getShort(BluetoothDevice.EXTRA_RSSI);
                int iRssi = abs(rssi);
                double power = (iRssi - 59) / 25.0;
                String mm = new Formatter().format("%.2f", pow(10, power)).toString();

                if(isNameValid(deviceName))
                {
                    String phone = deviceName.substring(11);
                    myDb.insertData(phone, time);
                    Log.i(TAG, "device added :" +phone+" "+time+":"+calendar.get(Calendar.MILLISECOND));

                    // for updating contacts list in Tab1
                    Intent intent1 = new Intent("ACTION_update_list");
                    intent1.putExtra("distance", mm);
                    intent1.putExtra("phone", phone);
                    intent1.putExtra("time", time);
                    getApplication().sendBroadcast(intent1);

                    Intent intent2 = new Intent("ACTION_update_contacts_today");
                    getApplication().sendBroadcast(intent2);
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Log.i(TAG, "ACTION_DISCOVERY_FINISHED");
                bluetoothAdapter.startDiscovery();
            }
        }
    };

    private boolean isNameValid(String name)
    {
        return name.contains(AppId);
    }
}
