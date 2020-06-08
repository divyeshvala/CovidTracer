
/* This background service will discover all devices
 *  and add them to local storage.
 */

package com.example.ctssd.Services;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.ctssd.Activities.Main2Activity;
import com.example.ctssd.R;
import com.example.ctssd.Utils.DatabaseHelper;
import com.example.ctssd.Utils.UserQueueObject;
import com.example.ctssd.Utils.Utilities;

import java.util.Calendar;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static java.lang.Math.abs;
import static java.lang.Math.pow;

public class BackgroundService extends Service
{
    public static final String CHANNEL_ID = "BackgroundServiceChannel";
    private static final String TAG = "BackgroundService";
    private static final String AppId = "c1t2";
    private static final int CROWD_LIMIT = 4; // TODO: change it to 7 later
    private static final int CROWD_TIME_LIMIT = 60000;
    private static final int CROWD_INSTANCES_LIMIT = 1; // TODO: Change it later
    private static long preCrowdInstanceTime = -1;
    private static int i=1, maxRiskFromContacts=0;
    private String location="null";
    private double latitude=-1, longitude=-1;
    private DatabaseHelper myDb;
    private BluetoothAdapter bluetoothAdapter;
    private Queue<UserQueueObject> contacts;
    private Set<String> contactsSet;
    private static boolean isUserMoving = false;

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        myDb = new DatabaseHelper(getApplicationContext());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        contacts = new LinkedList<>();
        contactsSet = new HashSet<>();

        SharedPreferences settings1 = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        //int contactsToday = settings1.getInt("", 0);

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, Main2Activity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                123, notificationIntent, 0);

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.main_notif_layout);
        contentView.setImageViewResource(R.id.image, R.drawable.ic_transfer_within_a_station_black_24dp);
        contentView.setTextViewText(R.id.title, "Covid Tracer");
        contentView.setTextViewText(R.id.text, "App is running in background.");

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                //.setContentTitle("Covid Tracer")
                .setSmallIcon(R.drawable.ic_transfer_within_a_station_black_24dp)
                .setTicker("Hey")
                .setContentIntent(pendingIntent)
                .setColor(Color.BLUE)
                .setContent(contentView)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText("App is running in background.\n"))
                //.setSmallIcon(R.drawable.icon_state)
                //.setContentIntent(pendingIntent)
                .build();

        startForeground(12, notification);

        // TODO: testing
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
        {
            Intent intent12 = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
            sendBroadcast(intent12);
        }

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        //TODO: testing
        IntentFilter intentFilt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(turnOnBluetooth, intentFilt);

        // Register for broadcasts when discovery is finished so that we can start it again.
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter2);
        bluetoothAdapter.startDiscovery();

        IntentFilter intentFilter3 = new IntentFilter("COORDINATES_FOUND");
        registerReceiver(locationReceiver, intentFilter3);

        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        Log.i(TAG, "Sheduler has been called");

                        if(bluetoothAdapter.getState()==BluetoothAdapter.STATE_ON)
                        {
                            Log.i(TAG, "BT is on");
                            SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                            float totalBTOnTime = settings.getFloat("totalBTOnTime", 0);
                            totalBTOnTime += 0.25;

                            SharedPreferences.Editor edit = settings.edit();
                            edit.putFloat("totalBTOnTime", totalBTOnTime);
                            if(i>=0) //TODO
                            {
                                if(isUserMoving && BluetoothAdapter.getDefaultAdapter().getState()!=BluetoothAdapter.STATE_ON)
                                {
                                    edit.putInt("bluetoothPenalty", (settings.getInt("bluetoothPenalty", 0)+1));
                                }
                                Intent intent = new Intent("ACTION_UPDATE_RISK_HALF_HOUR");
                                intent.putExtra("maxRiskFromContacts", maxRiskFromContacts);
                                getApplication().sendBroadcast(intent);
                                edit.putInt("maxRiskFromContacts", 0);
                                maxRiskFromContacts = 0;
                            }
                            edit.apply();
                            Log.i(TAG, "TotalBTOnTime :"+totalBTOnTime);
                            i++;
                        }
                    }
                }, 2, 2, TimeUnit.MINUTES); //TODO
        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private BroadcastReceiver turnOnBluetooth = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.i(TAG, "turnOnBluetooth receiver is called");
            if(BluetoothAdapter.getDefaultAdapter().getState()!=BluetoothAdapter.STATE_ON)
            {
                Log.i(TAG, "Inside on activity result : BT is off");
                Utilities utilities = new Utilities();
                utilities.sendNotifToTurnOnBT(getApplication());
            }
            else
            {
                Log.i(TAG, "Inside on activity result : BT is on now");
                // dismiss notification
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(1245);
            }
        }
    };

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            Log.i(TAG, "inside locationReceiver :" + action);
            if (action!=null)
            {
                // TODO: testing
                double preLatitude=latitude, preLongitude=longitude;
                location = intent.getStringExtra("coordinates");
                latitude = intent.getDoubleExtra("latitude", -1);
                longitude = intent.getDoubleExtra("longitude",-1);
                Log.i("COORDINATES", location);
                double dist=0;
                if(preLatitude!=-1 && preLongitude!=-1 && longitude!=-1 && latitude!=-1)
                    dist = distance(preLatitude, preLongitude, latitude, longitude)*(1000/0.62137);
                Log.i("COORDINATES", "Distance :"+dist);

                if(dist>=20)
                {
                    isUserMoving = true;
                }

                Utilities utilities = new Utilities();
                utilities.sendNotification(context, "Location", "You have travelled "+dist+"meters distace in past 30seconds.\n"+"Your coordinates : "+location);
            }
        }
    };

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
                if(device!=null && !device.getName().equals("NA"))
                    deviceName = device.getName();
                else
                    return;
                String time = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);

                short rssi = Objects.requireNonNull(intent.getExtras()).getShort(BluetoothDevice.EXTRA_RSSI);
                int iRssi = abs(rssi);
                double power = (iRssi - 59) / 25.0;
                String mm = new Formatter().format("%.2f", pow(10, power)).toString();
                int distance = convertToInt(mm);

                Log.i(TAG, "device found :" +deviceName+" "+time);

                if(deviceName!=null && isNameValid(deviceName) && distance<=2)
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

                    if(  myDb.getRiskOfContact(phone)<riskIndex )
                    {
                        // todo: check this, and on refresh risk is not coming from maxContactsRisk...
                        Log.i(TAG, "preRisk of device not equal");
                        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                        SharedPreferences.Editor edit = settings.edit();
                        edit.putInt("contactsTodayPenalty", (settings.getInt("contactsTodayPenalty", 0)+1));
                        edit.apply();
                    }

                    myDb.insertData(phone, time, riskIndex, location);
                    Log.i(TAG, "device added :" +phone+"  "+time+"  "+riskIndex);

                    if(riskIndex>maxRiskFromContacts)
                    {
                        maxRiskFromContacts = riskIndex;
                        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                        SharedPreferences.Editor edit = settings.edit();
                        edit.putInt("maxRiskFromContacts", maxRiskFromContacts);
                        edit.apply();
                    }

                    checkIfUserInTheCrowd(phone);
                    Log.i(TAG, "Distance less than four");

                    // for updating contacts list in Tab1
                    Intent intent1 = new Intent("ACTION_update_list");
                    intent1.putExtra("distance", mm);
                    intent1.putExtra("phone", phone);
                    intent1.putExtra("time", time);
                    intent1.putExtra("riskIndex", riskIndex);
                    getApplication().sendBroadcast(intent1);

                    Intent intent2 = new Intent("ACTION_UPDATE_CONTACTS");
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

    private void checkIfUserInTheCrowd(String phone)
    {
        Calendar now = Calendar.getInstance();
        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        preCrowdInstanceTime = settings.getLong("preCrowdInstanceTime", -1);
        if(preCrowdInstanceTime!=-1 && (now.getTime().getTime()-preCrowdInstanceTime<600000))
        {
            return;
        }

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
                        int crowdInstances = settings.getInt("crowdInstances", 0);
                        crowdInstances++;

                        if( crowdInstances >= CROWD_INSTANCES_LIMIT )
                        {
                            SharedPreferences.Editor edit = settings.edit();
                            edit.putInt("crowdInstances", 0);
                            edit.putInt("crowdNo", (settings.getInt("crowdNo", 0)+1));
                            edit.putLong("preCrowdInstanceTime", now.getTime().getTime());
                            edit.apply();
                        }
                        else
                        {
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putInt("crowdInstances", crowdInstances);
                            editor.apply();
                        }
                        preCrowdInstanceTime = now.getTime().getTime();
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
        if(riskIndex==null) return 0;
        int n=0;
        for(int i=0; i<riskIndex.length(); i++)
        {
            if(riskIndex.charAt(i)>'9' || riskIndex.charAt(i)<'0')
                return 0;
            n = n*10 + (riskIndex.charAt(i)-'0');
        }
        return n;
    }

    private boolean isNameValid(String name)
    {
        return name.contains(AppId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            // don't forget to unregister receiver
            unregisterReceiver(turnOnBluetooth);
            unregisterReceiver(locationReceiver);
            unregisterReceiver(receiver);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        Log.i("tag", "onTask removed called.");
        Toast.makeText(getApplicationContext(), "onTask removed called.", Toast.LENGTH_LONG).show();
        Intent restartServiceTask = new Intent(getApplicationContext(), this.getClass());
        restartServiceTask.setPackage(getPackageName());
        PendingIntent restartPendingIntent = PendingIntent.getService(getApplicationContext(), 12, restartServiceTask, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager myAlarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        myAlarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartPendingIntent);

        super.onTaskRemoved(rootIntent);
    }


    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}
