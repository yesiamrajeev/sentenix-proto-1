package com.example.sentenix_proto_1.Fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.sentenix_proto_1.ChatActivity;
import com.example.sentenix_proto_1.Demoactivity;
import com.example.sentenix_proto_1.LocationUpdateService;
import com.example.sentenix_proto_1.LoginActivity;
import com.example.sentenix_proto_1.ProfileActivity;
import com.example.sentenix_proto_1.R;
import com.example.sentenix_proto_1.UploadActivity;
import com.example.sentenix_proto_1.update.ConfigApplier;
import com.example.sentenix_proto_1.update.ConfigStore;
import com.example.sentenix_proto_1.update.UpdateClient;
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
    private static final String TAG = "HomeFragment";
    private static final long MIN_CHECKING_MS = 4000L;
    private static final long UPDATING_MS = 5000L;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;
    private static final double KHANDAGIRI_LAT = 20.2569;
    private static final double KHANDAGIRI_LONG = 85.7792;
    private static final double KIITSQUARE_LAT = 20.3534;
    private static final double KIITSQUARE_LONG = 85.8268;

    private View rootView;
    private SwitchCompat roleToggle;
    private final UpdateClient updateClient = new UpdateClient();
    private ConfigStore configStore;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        rootView = view;
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        configStore = new ConfigStore(requireContext());

        Button navigateToMainButton = view.findViewById(R.id.navigate_to_main_button);
        Button sendSosButton = view.findViewById(R.id.send_sos_button);
        Button uplbtn = view.findViewById(R.id.uplbtn);
        Button toprofile= view.findViewById(R.id.profileactivity);
        Button checkUpdateButton = view.findViewById(R.id.checkUpdateButton);
        roleToggle = view.findViewById(R.id.roleToggle);

        navigateToMainButton.setOnClickListener(this);
        sendSosButton.setOnClickListener(this);
       uplbtn.setOnClickListener(this);
        toprofile.setOnClickListener(this);
        checkUpdateButton.setOnClickListener(this);

        roleToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                roleToggle.setText(isChecked ? "Role: admin" : "Role: user");
            }
        });

        Intent serviceIntent = new Intent(requireContext(), LocationUpdateService.class);
        requireActivity().startService(serviceIntent);
        Toast.makeText(requireContext(), "Location Update Service Started...", Toast.LENGTH_SHORT).show();

        reapplyPersistedConfig();

        return view;
    }

    private void reapplyPersistedConfig() {
        if (!configStore.isApplied()) return;
        String xml = configStore.getAppliedXml();
        if (xml == null) return;
        try {
            ConfigApplier.Config cfg = ConfigApplier.parse(xml);
            ConfigApplier.apply(rootView, cfg);
        } catch (Exception e) {
            Log.w(TAG, "failed to reapply persisted config", e);
        }
    }

    private String currentRole() {
        return (roleToggle != null && roleToggle.isChecked())
                ? UpdateClient.ROLE_ADMIN
                : UpdateClient.ROLE_USER;
    }

    private void onCheckForUpdateClicked() {
        final ProgressDialog checking = new ProgressDialog(requireContext());
        checking.setMessage("Checking for updates...");
        checking.setCancelable(false);
        checking.show();

        final long startedAt = System.currentTimeMillis();
        final String role = currentRole();

        updateClient.checkForUpdate(role, new UpdateClient.Listener() {
            @Override
            public void onResult(@NonNull UpdateClient.Result result) {
                runAfterMinWait(startedAt, () -> {
                    if (!isAdded()) return;
                    checking.dismiss();
                    if (result.hasUpdate()) {
                        promptUpdateAvailable(result.body);
                    } else if (result.statusCode == 204) {
                        Toast.makeText(requireContext(), "No updates available", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                "Unexpected response: " + result.statusCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Throwable error) {
                Log.w(TAG, "check-update failed", error);
                runAfterMinWait(startedAt, () -> {
                    if (!isAdded()) return;
                    checking.dismiss();
                    Toast.makeText(requireContext(),
                            "Update check failed: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void runAfterMinWait(long startedAt, Runnable r) {
        long elapsed = System.currentTimeMillis() - startedAt;
        long remaining = Math.max(0L, MIN_CHECKING_MS - elapsed);
        mainHandler.postDelayed(r, remaining);
    }

    private void promptUpdateAvailable(@NonNull final String responseBody) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Update Available")
                .setMessage("A new configuration is available. Apply now?")
                .setPositiveButton("Update Now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        runUpdate(responseBody);
                    }
                })
                .setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void runUpdate(@NonNull final String responseBody) {
        final ProgressDialog updating = new ProgressDialog(requireContext());
        updating.setMessage("Updating...");
        updating.setCancelable(false);
        updating.show();

        mainHandler.postDelayed(() -> {
            if (!isAdded()) return;
            try {
                String xml = ConfigApplier.extractXmlFromEnvelope(responseBody);
                ConfigApplier.Config cfg = ConfigApplier.parse(xml);
                ConfigApplier.apply(rootView, cfg);
                configStore.save(xml, 2);
                updating.dismiss();
                Toast.makeText(requireContext(), "Updated", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "apply failed", e);
                updating.dismiss();
                Toast.makeText(requireContext(),
                        "Update failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }, UPDATING_MS);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.navigate_to_main_button) {
            navigateToMainActivity();
        } else if (v.getId() == R.id.send_sos_button) {

            sendSOS();
        }else if(v.getId() == R.id.uplbtn){
            ulpbtn();
        }else if(v.getId()==R.id.profileactivity) {
            toprofilevisit();
        }else if(v.getId()==R.id.checkUpdateButton) {
            onCheckForUpdateClicked();
        }
    }

    private void toprofilevisit() {
        Intent i = new Intent(getActivity(), ProfileActivity.class);
        startActivity(i);
        requireActivity().finish();
    }

    private void ulpbtn() {
//        Intent i = new Intent(getActivity(), Demoactivity.class);
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
                                    if (distance < 1.0) {
                                        String cleaned = sanitizePhone(phoneNumber);
                                        if (cleaned == null) {
                                            Log.w(TAG, "skipping invalid SOS recipient (raw=" + phoneNumber + ")");
                                        } else {
                                            try {
                                                SmsManager smsManager = SmsManager.getDefault();
                                                smsManager.sendTextMessage(cleaned, null, sosMessage, null, null);
                                            } catch (IllegalArgumentException e) {
                                                Log.w(TAG, "SMS send rejected for " + cleaned, e);
                                            }
                                        }
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

    /**
     * Returns a phone number safe to hand to SmsManager, or null if it's unusable.
     * Strips spaces, dashes, dots, parentheses; requires optional leading '+' and 7-15 digits.
     */
    private static String sanitizePhone(String raw) {
        if (raw == null) return null;
        String stripped = raw.replaceAll("[\\s\\-().]", "");
        if (stripped.matches("\\+?[0-9]{7,15}")) {
            return stripped;
        }
        return null;
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
