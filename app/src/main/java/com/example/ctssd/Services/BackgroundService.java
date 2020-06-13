
/* This background service will discover all devices
 *  and add them to local storage.
 */

package com.example.ctssd.Services;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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

public class BackgroundService extends Service {
    public static final String CHANNEL_ID = "BackgroundServiceChannel";
    private static final String TAG = "BackgroundService";
    private static final String AppId = "c1t2";
    private static final int CROWD_LIMIT = 6;
    private static final int CROWD_TIME_LIMIT = 60000;
    private static final int CROWD_INSTANCES_LIMIT = 2;
    private static long preCrowdInstanceTime = -1;
    private static int i = 1, maxRiskFromContacts = 0;
    private double latitude = -1, longitude = -1;
    private DatabaseHelper myDb;
    private BluetoothAdapter bluetoothAdapter;
    private Queue<UserQueueObject> contacts;
    private Set<String> contactsSet;
    private static boolean isUserMoving = false;
    private static String deviceId;
    private static int contactsTodayGlobal = 0, riskIndexGlobal = 0;
    private String temp="";

    private Alarm alarm = new Alarm();

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location)
        {
            Log.i("LocationBackgr", "onLocationChanged");

            double preLatitude = latitude;
            double preLongitude = longitude;

            latitude = location.getLatitude();
            longitude = location.getLongitude();
            onNewLocation(preLatitude, preLongitude);
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

    private void onNewLocation(double preLatitude, double preLongitude)
    {
        double dist=0, dist2=0;
        if(preLatitude!=-1 && preLongitude!=-1 && longitude!=-1 && latitude!=-1)
        {
            Location start = new Location("");
            start.setLatitude(preLatitude);
            start.setLongitude(preLongitude);
            Location dest = new Location("");
            dest.setLatitude(latitude);
            dest.setLongitude(longitude);

            dist = start.distanceTo(dest);

            //dist2 = distance(preLatitude, preLongitude, latitude, longitude)*(1000/0.62137);
        }
        Log.i("COORDINATES", "Distance :"+dist);

        if(dist>=20)
        {
            isUserMoving = true;
        }

        //Utilities utilities = new Utilities();
        //utilities.sendNotification(getApplicationContext(), "Location", "You have travelled "+dist+"meters distance in past 30seconds.\n"+"Your coordinates : "+latitude+"\n"+longitude,
               // 311, "locationTemp1");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Inside onCreate...");
        Toast.makeText(this, "inside onCreate", Toast.LENGTH_SHORT).show();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        //temp += Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+"=";
                        Log.i(TAG, "Sheduler has been called "+temp);

                        //Utilities utilities = new Utilities();
                        //utilities.sendTempNotification(getApplication(), "BTOnTime", temp, 111, "tempC");

                        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                        SharedPreferences.Editor edit = settings.edit();
                        if(bluetoothAdapter.getState()==BluetoothAdapter.STATE_ON)
                        {
                            Log.i(TAG, "BT is on");
                            float totalBTOnTime = settings.getFloat("totalBTOnTime", 0);
                            totalBTOnTime += 0.50;
                            edit.putFloat("totalBTOnTime", totalBTOnTime);
                            Log.i(TAG, "TotalBTOnTime :"+totalBTOnTime);
                        }
                        else if(isUserMoving)
                        {
                            edit.putInt("bluetoothPenalty", settings.getInt("bluetoothPenalty", 0)+1);
                        }
                        int preMaxRFC = settings.getInt("maxRiskFromContacts", 0);
                        if( maxRiskFromContacts>preMaxRFC )
                        {
                            edit.putInt("maxRiskFromContacts", maxRiskFromContacts);
                            maxRiskFromContacts = 0;
                        }
                        if(isUserMoving && BluetoothAdapter.getDefaultAdapter().getState()!=BluetoothAdapter.STATE_ON)
                        {
                            edit.putInt("bluetoothPenalty", (settings.getInt("bluetoothPenalty", 0)+1));
                            isUserMoving=false;
                        }
                        edit.apply();
                        findRiskIndex();

                        if(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)>=22)
                        {
                            Intent intent1 = new Intent("ACTION_STOP_SERVICE");
                            sendBroadcast(intent1);
                        }
                    }
                }, 30, 30, TimeUnit.MINUTES); //todo

    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        myDb = new DatabaseHelper(getApplicationContext());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        deviceId = settings.getString("myDeviceId", "NA");
        riskIndexGlobal = settings.getInt("myRiskIndex", 0);
        Cursor cursor = myDb.getAllData();
        if (cursor != null) {
            contactsTodayGlobal = cursor.getCount() + settings.getInt("contactsTodayPenalty", 0);
            cursor.close();
        }

        contacts = new LinkedList<>();
        contactsSet = new HashSet<>();

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

        updateNotification();
        setupLocation();

        // TODO: stop service in the night. (alarmManager/ workManager).
        //alarm.setAlarm(this);

        if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
        {
            Intent intent12 = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
            sendBroadcast(intent12);
        }

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        IntentFilter intentFilt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(turnOnBluetooth, intentFilt);

        // Register for broadcasts when discovery is finished so that we can start it again.
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter2);
        bluetoothAdapter.startDiscovery();

        IntentFilter intentFilter3 = new IntentFilter("COORDINATES_FOUND");
        registerReceiver(locationReceiver, intentFilter3);

        IntentFilter intentFilter5 = new IntentFilter("ACTION_STOP_SERVICE");
        registerReceiver(stopServiceReceiver, intentFilter5);

        //return START_REDELIVER_INTENT;  //TODO: START_STICKY
        return START_STICKY;
    }

    public void setupLocation()
    {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            Log.i("LocationTab2", "requestLocUpdates1.1");
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Log.i("LocationTab2", "requestLocUpdates1.2");
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 120000, 20, locationListener);
            } // todo:
        }
        else
        {
            Log.i("LocationTab2", "requestLocUpdates2");
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 120000, 20, locationListener);
        }

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//            Log.i("LocationTab21", "requestLocUpdates1.1");
//            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
            Log.i(TAG, "inside COORDINATES_FOUND :" + action);
            if (action!=null)
            {
                double preLatitude=latitude, preLongitude=longitude;
                latitude = intent.getDoubleExtra("latitude", -1);
                longitude = intent.getDoubleExtra("longitude",-1);
                Log.i("COORDINATES", "from broadcast");
            }
        }
    };

    private final BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            Log.i(TAG, "inside stopServiceReceiver :" + action);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(112);

            if(BluetoothAdapter.getDefaultAdapter().getState()!=BluetoothAdapter.STATE_ON)
            {
                notificationManager.cancel(1245);
            }

            stopForeground(true);
            stopSelf();
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
                String deviceName ;
                if(device==null)
                    return;
                deviceName = device.getName();
                if(deviceName==null )
                    return;
                if(deviceName.equals("NA"))
                    return;

                String time = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);
                short rssi = Objects.requireNonNull(intent.getExtras()).getShort(BluetoothDevice.EXTRA_RSSI);
                int iRssi = abs(rssi);
                double power = (iRssi - 59) / 25.0;
                String mm = new Formatter().format("%.2f", pow(10, power)).toString();
                int distance = convertToInt(mm);

                Log.i(TAG, "device found :" +deviceName+" "+time);

                if(isNameValid(deviceName) && distance<=2)
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

                    SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                    Cursor cursor2 = myDb.getAllData();

                    if( checkTimeDiffAndRisk(cursor2, phone, riskIndex) )
                    {
                        Log.i(TAG, "preRisk of device not equal and half hour is up.");
                        SharedPreferences.Editor edit = settings.edit();
                        edit.putInt("contactsTodayPenalty", (settings.getInt("contactsTodayPenalty", 0)+1));
                        edit.apply();
                    }

                    myDb.insertData(phone, time, riskIndex, latitude+"_"+longitude);
                    Log.i(TAG, "device added :" +phone+"  "+time+"  "+riskIndex);

                    if(riskIndex>maxRiskFromContacts)
                    {
                        maxRiskFromContacts = riskIndex;
                    }

                    checkIfUserInTheCrowd(phone);
                    Log.i(TAG, "Distance less than four");

                    int tempContactsToday = 0;
                    Cursor cursor = myDb.getAllData();
                    if(cursor!=null) {
                        tempContactsToday = cursor.getCount() + settings.getInt("contactsTodayPenalty", 0);
                        cursor.close();
                        if (tempContactsToday != contactsTodayGlobal) {
                            contactsTodayGlobal = tempContactsToday;
                            Intent intent2 = new Intent("ACTION_UPDATE_CONTACTS");
                            getApplication().sendBroadcast(intent2);

                            updateNotification();
                        }
                    }
                    // for updating contacts list in Tab1
                    Intent intent1 = new Intent("ACTION_update_list");
                    intent1.putExtra("distance", mm);
                    intent1.putExtra("phone", phone);
                    intent1.putExtra("time", time);
                    intent1.putExtra("riskIndex", riskIndex);
                    getApplication().sendBroadcast(intent1);
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Log.i(TAG, "ACTION_DISCOVERY_FINISHED");
                bluetoothAdapter.startDiscovery();
            }
        }
    };

    private void updateNotification()
    {
        Utilities utilities = new Utilities();
        utilities.sendStatsUpdateNotification(getApplication(), riskIndexGlobal, contactsTodayGlobal);
    }

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

                        if( crowdInstances > CROWD_INSTANCES_LIMIT )
                        {
                            SharedPreferences.Editor edit = settings.edit();
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

    public void findRiskIndex()
    {
        SharedPreferences settings = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        SharedPreferences.Editor mapEditor = settings.edit();

        int maxRiskIndexVar = settings.getInt("maxRiskIndexVar", 0);

        // 1. max risk factors of contacts (scaled on 20)
        int fromContactsRiskMax = settings.getInt("fromContactsRiskMax", 0);
        int maxRFC = settings.getInt("maxRiskFromContacts", 0);
        if(maxRiskIndexVar<maxRFC)
        {
            if(fromContactsRiskMax<(3*maxRFC)/10){
                fromContactsRiskMax = (3*maxRFC)/10;
            }
        }
        mapEditor.putInt("fromContactsRiskMax", fromContactsRiskMax);

        //2. contacts today (But really yesterday)
        int fromContactsToday=0;
        Cursor cursor = myDb.getAllData();
        if(cursor!=null)
        {
            fromContactsToday = cursor.getCount() + settings.getInt("contactsTodayPenalty", 0);
        }
        if(fromContactsToday>10)
        {
            if((fromContactsToday-10) <= 30)
                fromContactsToday = (fromContactsToday-10);
            else
                fromContactsToday = 30;
        }
        else
            fromContactsToday = 0;
        mapEditor.putInt("fromContactsToday", fromContactsToday);

        //3. Numbers of hours user's bluetooth was off.
        Utilities utilities = new Utilities();
        int BluetoothOffTime = (int) utilities.getTotalBluetoothOffTime(this);
        int fromBluetoothOffTime = settings.getInt("bluetoothPenalty", 0);
        fromBluetoothOffTime += (BluetoothOffTime*0.5) + settings.getInt("bluetoothPenalty",0);
        if(fromBluetoothOffTime>15)
            fromBluetoothOffTime = 15;
        mapEditor.putInt("fromBluetoothOffTime", fromBluetoothOffTime);

        //4. Number of times user was standing in crowd.
        int CrowdNo = settings.getInt("crowdNo", 0);
        int fromCrowdInstances=0;
        if(CrowdNo>2)
            fromCrowdInstances = (CrowdNo-2)*5;
        if(fromCrowdInstances>25)
            fromCrowdInstances = 25;
        mapEditor.putInt("fromCrowdInstances", fromCrowdInstances);
        mapEditor.apply();

        // 5. from Self assessment report.
//        int fromSelfAssessReport = settings.getInt("riskFromReport", 0);
//        messageForRiskIndex.put("fromSelfAssessReport", fromSelfAssessReport);

        int riskIndex = fromContactsRiskMax+fromBluetoothOffTime+fromContactsToday+fromCrowdInstances;
        if(riskIndex>100)
            riskIndex = 100;

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("myRiskIndex", riskIndex);
        if(maxRiskIndexVar<riskIndex)
        {
            maxRiskIndexVar = riskIndex;
            editor.putInt("maxRiskIndexVar", maxRiskIndexVar);
            bluetoothAdapter.setName(AppId+deviceId+"_"+maxRiskIndexVar);
            Log.i(TAG, "Your name changed : "+bluetoothAdapter.getName());
        }
        editor.apply();

        // send broadcast to update riskIndex view and graph
        Intent intent2 = new Intent("ACTION_UPDATE_RISK_GRAPH");
        intent2.putExtra("riskIndex", riskIndex);
        sendBroadcast(intent2);

        Intent intent = new Intent("ACTION_UPDATE_RISK_HALF_HOUR");
        intent.putExtra("riskIndex", riskIndex);
        sendBroadcast(intent);

        Log.i("Notification", "sending you risk notif");
        //Toast.makeText(this, "Sending you risk notification", Toast.LENGTH_SHORT).show();

        if(riskIndex!=riskIndexGlobal)
        {
            riskIndexGlobal = riskIndex;
            updateNotification();
        }
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
            unregisterReceiver(stopServiceReceiver);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private boolean checkTimeDiffAndRisk(Cursor cursor, String DId, int risk)
    {
        while(cursor!=null && cursor.moveToNext())
        {
            if(cursor.getString(0).equals(DId))
            {
                String time = cursor.getString(1);
                if(getTimeDiff(time))
                {
                    if(cursor.getInt(2)<risk) {
                        cursor.close();
                        return true;
                    }
                    cursor.close();
                    return false;
                }
                else {
                    cursor.close();
                    return false;
                }
            }
        }
        Objects.requireNonNull(cursor).close();
        return false;
    }

    private boolean getTimeDiff(String time)
    {
        int i;
        for(i=0; i<time.length(); i++)
            if(time.charAt(i)==':')
                break;
        int hour = convertToInt(time.substring(0, i));
        int minute = convertToInt(time.substring(i+1));
        Log.i("TimeDiff", "Pre time :"+ hour+":"+minute);
        if(hour==Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        {
            return Calendar.getInstance().get(Calendar.MINUTE) - minute >= 30;
        }
        else if(hour==Calendar.getInstance().get(Calendar.HOUR_OF_DAY)-1)
        {
            return Calendar.getInstance().get(Calendar.MINUTE) + 60 - minute >= 30;
        }
        return true;
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
}
