
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
import com.example.ctssd.Utils.UserQueueObject;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

public class BackgroundService extends Service
{
    private static final String TAG = "BackgroundService";
    private static final String AppId = "c1t2";
    private static final int CROWD_LIMIT = 2;  // TODO: change it to 7 later
    private static final int CROWD_TIME_LIMIT = 60000;

    private DatabaseHelper myDb;
    private BluetoothAdapter bluetoothAdapter;

    public static Calendar startTime;
    private Queue<UserQueueObject> contacts;
    private Set<String> contactsSet;
    private int counter;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        myDb = new DatabaseHelper(getApplicationContext());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        startTime = Calendar.getInstance();
        contacts = new LinkedList<>();
        contactsSet = new HashSet<>();

        counter = 0;

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        // Register for broadcasts when discovery is finished so that we can start it again.
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter2);

        bluetoothAdapter.startDiscovery();

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
            // don't forget to unregister receiver
            unregisterReceiver(receiver);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Calendar calendar = Calendar.getInstance();
                String deviceName = null;
                if(device!=null)
                    deviceName = device.getName();
                String time = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);

                short rssi = Objects.requireNonNull(intent.getExtras()).getShort(BluetoothDevice.EXTRA_RSSI);
                int iRssi = abs(rssi);
                double power = (iRssi - 59) / 25.0;
                String mm = new Formatter().format("%.2f", pow(10, power)).toString();

                Log.i(TAG, "device found :" +deviceName+" "+time);

                if(deviceName!=null && isNameValid(deviceName))
                {
                    // get phone and riskIndex from deviceName
                    int i;
                    for(i=AppId.length(); i<deviceName.length(); i++)
                    {
                        if(deviceName.charAt(i)=='_')
                            break;
                    }
                    String phone = deviceName.substring(AppId.length(), i);
                    int riskIndex = convertToInt(deviceName.substring(i+1));

                    checkIfUserInTheCrowd(phone);

                    myDb.insertData(phone, time, riskIndex);
                    Log.i(TAG, "device added :" +phone+"  "+time+"  "+riskIndex);

                    // for updating contacts list in Tab1
                    Intent intent1 = new Intent("ACTION_update_list");
                    intent1.putExtra("distance", mm);
                    intent1.putExtra("phone", phone);
                    intent1.putExtra("time", time);
                    intent1.putExtra("riskIndex", riskIndex);
                    getApplication().sendBroadcast(intent1);

                    Intent intent2 = new Intent("ACTION_UPDATE_CONTACTS_AND_RISK");
                    getApplication().sendBroadcast(intent2);
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Log.i(TAG, "ACTION_DISCOVERY_FINISHED");
                counter++;
                if(counter%2==0)
                {
                    Intent intent3 = new Intent("REMOVE_OUT_OF_RANGE_DEVICES");
                    getApplication().sendBroadcast(intent3);
                }
                bluetoothAdapter.startDiscovery();
            }
        }
    };

    private void checkIfUserInTheCrowd(String phone)
    {
        Calendar now = Calendar.getInstance();
        Log.i(TAG, "CheckIfInTheCrowd : now"+now.getTime());
        if(!contactsSet.contains(phone))
        {
            contactsSet.add(phone);
            contacts.add(new UserQueueObject(phone, now));
            UserQueueObject head = contacts.peek();
            if(head!=null)
            {
                Log.i(TAG, "CheckIfInTheCrowd : head time ="+ head.getCalendar().getTime());
                if(now.getTime().getTime()-head.getCalendar().getTime().getTime() <= CROWD_TIME_LIMIT )
                {
                    if(contacts.size()>=CROWD_LIMIT)
                    {
                        contacts.clear();
                        contactsSet.clear();
                        Log.i(TAG, "Crowd limit reached");
                        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                        int crowdInstances = settings.getInt("crowdInstances", 0);
                        crowdInstances++;
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt("crowdInstances", crowdInstances);
                        editor.apply();
                    }
                }
                else
                {
                    contactsSet.remove(head.getPhone());
                    contacts.remove();
                    UserQueueObject qHead = contacts.peek();
                    while(qHead!=null && now.getTime().getTime()-qHead.getCalendar().getTime().getTime() > CROWD_TIME_LIMIT)
                    {
                        contactsSet.remove(qHead.getPhone());
                        contacts.remove();
                        qHead = contacts.peek();
                    }
                }
            }
        }
        Log.i(TAG, "CheckIfInTheCrowd : size of contacts = "+contacts.size());
    }

    private int convertToInt(String riskIndex)
    {
        if(riskIndex==null) return -1;  // TODO: change it to zero later
        int n=0;
        for(int i=0; i<riskIndex.length(); i++)
        {
            if(riskIndex.charAt(i)>'9' || riskIndex.charAt(i)<'0')
                return -1;   // TODO: change it to zero later
            n = n*10 + (riskIndex.charAt(i)-'0');
        }
        return n;
    }

    private boolean isNameValid(String name)
    {
        return name.contains(AppId);
    }



}
