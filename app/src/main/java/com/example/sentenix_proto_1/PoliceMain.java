package com.example.sentenix_proto_1;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.sentenix_proto_1.Fragments.HomeFragment;
import com.example.sentenix_proto_1.Fragments.AdminHomeFragment;
import com.example.sentenix_proto_1.Fragments.AdminNotificationFragment;
import com.example.sentenix_proto_1.Fragments.PoliceHomeFragment;
import com.example.sentenix_proto_1.Fragments.PoliceNotificationFragment;
import com.example.sentenix_proto_1.Fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PoliceMain extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    FirebaseAuth mAuth;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.police_main);

        mAuth = FirebaseAuth.getInstance();
        bottomNavigationView = findViewById(R.id.bttom_nav_police);

        // Initialize the first fragment when activity is created
        loadFragment(new PoliceHomeFragment()); // Use constructor to create HomeFragment instance

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    loadFragment(new PoliceHomeFragment());
                } else if (id == R.id.nav_notifications) {
                    loadFragment(new PoliceNotificationFragment());
                } else if (id == R.id.nav_profile) {
                    loadFragment(new ProfileFragment());
                }
                return true; // Return true to indicate the item is selected
            }
        });

        // Fetch and display the username
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String username = currentUser.getDisplayName();
            TextView usernameTextView = findViewById(R.id.usernamepolice);
            usernameTextView.setText(""+username);
        }

    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerpolice, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
