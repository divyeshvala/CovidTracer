package com.example.ctssd.Utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
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
import java.util.Objects;

import static android.content.Context.LOCATION_SERVICE;

public class GetLocation
{
    private Context mContext;
    private FusedLocationProviderClient fusedLocationProviderClient;

    public GetLocation(Context context, FusedLocationProviderClient fusedLocationProviderClient) {
        this.mContext = context;
        this.fusedLocationProviderClient = fusedLocationProviderClient;
    }

    public void findLocation()
    {
        Log.i("LocationFused", "inside get location");
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(mContext), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i("LocationFused", "permission is there");
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    Log.i("LocationFused", "inside listener");
                    if (location != null)
                    {
                        Log.i("LocationFused", "location is not null");
                        getAddress(location);
                    } else {
                        Log.i("LocationFused", "location is null");
                        requestNewLocationData();
                    }
                }
            });
        } else {
            Log.i("LocationFused", "permission is not there");
            Intent intent = new Intent("GET_LOCATION_PERMISSION");
            mContext.sendBroadcast(intent);
        }
    }

    private void requestNewLocationData()
    {
        if (!isLocationEnabled_Network() && !isLocationEnabled_GPS())
        {
            Intent intent = new Intent("GPS_PERMISSION");
            mContext.sendBroadcast(intent);
        }
        else {
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            //mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
            mLocationRequest.setInterval(15000);
            mLocationRequest.setFastestInterval(15000);
            //mLocationRequest.setSmallestDisplacement(20);
            //mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            //mLocationRequest.setNumUpdates(1);

            Log.i("LocationLooper", "looper");
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(mContext));
            fusedLocationProviderClient.requestLocationUpdates(
                    mLocationRequest, mLocationCallback,
                    Looper.myLooper()
            );
        }
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Log.i("LocationCallback", "onLocResult");
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
        String coordinates = location.getLatitude() +"_"+ location.getLongitude();
        Intent intent1 = new Intent("COORDINATES_FOUND");
        intent1.putExtra("coordinates", coordinates);
        intent1.putExtra("latitude",location.getLatitude());
        intent1.putExtra("longitude", location.getLatitude());
        mContext.sendBroadcast(intent1);
    }

    private boolean isLocationEnabled_Network() {
        LocationManager locationManager = (LocationManager) Objects.requireNonNull(mContext).getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    private boolean isLocationEnabled_GPS() {
        LocationManager locationManager = (LocationManager) Objects.requireNonNull(mContext).getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
