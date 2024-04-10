package com.example.sentenix_proto_1.Fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.sentenix_proto_1.ChatActivity;
import com.example.sentenix_proto_1.LocationUpdateService;
import com.example.sentenix_proto_1.LoginActivity;
import com.example.sentenix_proto_1.R;
import com.example.sentenix_proto_1.UploadActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements View.OnClickListener {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;
    private static final double KHANDAGIRI_LAT = 20.2569;
    private static final double KHANDAGIRI_LONG = 85.7792;
    private static final double KIITSQUARE_LAT = 20.3534;
    private static final double KIITSQUARE_LONG = 85.8268;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        Button navigateToMainButton = view.findViewById(R.id.navigate_to_main_button);
        Button sendSosButton = view.findViewById(R.id.send_sos_button);
        Button uplbtn = view.findViewById(R.id.uplbtn);


        navigateToMainButton.setOnClickListener(this);
        sendSosButton.setOnClickListener(this);
       uplbtn.setOnClickListener(this);

        Intent serviceIntent = new Intent(requireContext(), LocationUpdateService.class);
        requireActivity().startService(serviceIntent);
        Toast.makeText(requireContext(), "Location Update Service Started...", Toast.LENGTH_SHORT).show();

        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.navigate_to_main_button) {
            navigateToMainActivity();
        } else if (v.getId() == R.id.send_sos_button) {

            sendSOS();
        }else if(v.getId() == R.id.uplbtn){
            ulpbtn();
        }
    }

    private void ulpbtn() {
        Intent i = new Intent(getActivity(), UploadActivity.class);
        startActivity(i);
        requireActivity().finish();
    }


    private void navigateToMainActivity() {
        Intent i = new Intent(getActivity(), ChatActivity.class);
        startActivity(i);
        requireActivity().finish();
    }

    private void sendSOS() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Confirm SOS");
            builder.setMessage("Are you sure you want to send an SOS message?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    String userId = currentUser.getUid();
                    String username = currentUser.getDisplayName();
                    fetchUserLocationAndSendSOS(userId, username);

                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "SOS Cancelled", Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
    public void onRetrieveButtonClick(double userLat, double userLong) {
        String sosMessage = "URGENT, Need Help. Click here for location: https://maps.google.com/maps?q=" + userLat + "," + userLong;
        DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference userLocationsReference = FirebaseDatabase.getInstance().getReference("userLocations");

        usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                userLocationsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userLocationsSnapshot) {
                        for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                            String userId = userSnapshot.getKey();
                            String phoneNumber = userSnapshot.child("phoneNumber").getValue(String.class);

                            // Get user's location from userLocations database
                            DataSnapshot userLocationSnapshot = userLocationsSnapshot.child(userId);
                            if (userLocationSnapshot.exists()) {
                                Double userLocationLat = userLocationSnapshot.child("latitude").getValue(Double.class);
                                Double userLocationLong = userLocationSnapshot.child("longitude").getValue(Double.class);

                                if (userLocationLat != null && userLocationLong != null) {
                                    // Calculate distance between user's location and the provided location
                                    double distance = calculateDistance2(userLat, userLong, userLocationLat, userLocationLong);
                                    // Check if distance is less than 1km (adjust threshold as needed)
                                    if (distance < 1.0 && phoneNumber != null) {
                                        // Send SMS to the user
                                        SmsManager smsManager = SmsManager.getDefault();
                                        smsManager.sendTextMessage(phoneNumber, null, sosMessage, null, null);
                                        // You may also consider handling success/failure here
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle errors
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors
            }
        });
    }

    private double calculateDistance2(double lat1, double lon1, Double lat2, Double lon2) {
        if (lat2 == null || lon2 == null) {
            // Handle the case where latitude or longitude is null
            return Double.MAX_VALUE; // Return a large value indicating invalid distance
        }

        // The Haversine formula to calculate distance between two points on Earth
        double R = 6371; // Earth's radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in kilometers
    }

    @SuppressLint("MissingPermission")
    private void fetchUserLocationAndSendSOS(String userId, String username) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double userLat = location.getLatitude();
                            double userLong = location.getLongitude();
                            String sosMessage = "   SOS from User: " + username + "\n   Live Location: https://maps.google.com/maps?q=" + userLat + "," + userLong;
                            sendSOSMessageToNearestDatabase(userLat, userLong, sosMessage);
                            onRetrieveButtonClick(userLat,userLong);
                        } else {
                            Toast.makeText(requireContext(), "Unable to retrieve location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void sendSOSMessageToNearestDatabase(double userLat, double userLong, String sosMessage) {
        double distanceToKhandagiri = calculateDistance(userLat, userLong, KHANDAGIRI_LAT, KHANDAGIRI_LONG);
        double distanceToKiitsquare = calculateDistance(userLat, userLong, KIITSQUARE_LAT, KIITSQUARE_LONG);

        DatabaseReference nearestDatabase;
        if (distanceToKhandagiri < distanceToKiitsquare) {
            nearestDatabase = mDatabase.child("khandagiri");
        } else {
            nearestDatabase = mDatabase.child("kiitsquare");
        }

        nearestDatabase.push().setValue(sosMessage)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "SOS Sent", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error sending SOS", Toast.LENGTH_SHORT).show());
    }

    private double calculateDistance(double userLat, double userLong, double targetLat, double targetLong) {
        double earthRadius = 6371.0;
        double latDistance = Math.toRadians(targetLat - userLat);
        double longDistance = Math.toRadians(targetLong - userLong);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(targetLat))
                * Math.sin(longDistance / 2) * Math.sin(longDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;
        return distance;
    }
}
