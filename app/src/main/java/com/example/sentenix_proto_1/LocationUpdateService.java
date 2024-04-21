package com.example.sentenix_proto_1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LocationUpdateService extends Service {

    private static final long UPDATE_INTERVAL = 6000; // 1 minute interval
    private static final long FASTEST_INTERVAL = 30000; // Fastest interval
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "LocationUpdateService";

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private LocationCallback locationCallback;
    private Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("userLocations");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        createLocationCallback();

        // Check location permission
        if (checkLocationPermission()) {
            startLocationUpdates();
            showToast("Location Update Service Started");
        } else {
            requestLocationPermission();
        }
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale((MainActivity) mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale((MainActivity) mContext, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            showToast("Location permission required to update location");
            Log.d(TAG, "Location permission request shown to the user");
        }
        ActivityCompat.requestPermissions((MainActivity) mContext,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateUserLocation(Location location) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String username = currentUser.getDisplayName(); // Get the username from FirebaseUser
            String phoneNumber = currentUser.getPhoneNumber(); // Get the phone number from FirebaseUser

            User user = new User(username, phoneNumber, location.getLatitude(), location.getLongitude(),false);

            Log.d(TAG, "Updating user location: " + user.getUsername() + " - " +
                    user.getPhoneNumber() + " - " + user.getLatitude() + "," + user.getLongitude());

            mDatabase.child(userId).setValue(user)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Location updated successfully");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Failed to update location: " + e.getMessage());
                        }
                    });
        } else {
            Log.e(TAG, "Current user is null");
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        updateUserLocation(location); // Update the user's location
                    }
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        showToast("Location Update Service Stopped");
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void showToast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
