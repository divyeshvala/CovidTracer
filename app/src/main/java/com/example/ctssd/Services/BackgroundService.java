package com.example.ctssd.Services;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.ctssd.Utils.DatabaseHelper;
import com.example.ctssd.Utils.Utilities;
import java.util.Calendar;

public class BackgroundService extends Service
{
    DatabaseHelper myDb;
    BluetoothAdapter bluetoothAdapter;



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        myDb = new DatabaseHelper(getApplicationContext());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                int i=0;
                while(true){
                    bluetoothAdapter.startDiscovery();

                    Log.i("service", "discovery started");
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // send broadcast to update device list.
                    i++;
                    if(i%4==0)
                    {
                        Intent intent = new Intent("ACTION_update_list");
                        getApplication().sendBroadcast(intent);
                    }
                }
            }
        }).start();
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
        try {
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
                String mac = device.getAddress();
                Calendar calendar = Calendar.getInstance();
                String time = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);

                //if(phone!=null && Utilities.isPhoneNoValid(phone))
                //{
                    myDb.insertData(mac, time);
                    Log.i("service", "device added :" +mac+" "+time+":"+calendar.get(Calendar.MILLISECOND));
                    Intent intent1 = new Intent("ACTION_update_contacts_today");
                    getApplication().sendBroadcast(intent1);
                //}
            }
        }
    };
}
