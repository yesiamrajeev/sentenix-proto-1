package com.example.sentenix_proto_1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminProfileActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    private Button logoutButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        logoutButton = findViewById(R.id.button2);

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });


        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String username = currentUser.getDisplayName();
            String userPhone = currentUser.getPhoneNumber();
            String userEmail = currentUser.getEmail();
            TextView UserNameTV = findViewById(R.id.tv_userName);
            TextView UserPhoneTV = findViewById(R.id.tv_userPhone);
            TextView UserEmailTV = findViewById(R.id.tv_userEmail);
            UserNameTV.setText("Name: " + username);
            UserPhoneTV.setText("Phone: "+userPhone);
            UserEmailTV.setText("Email: "+userEmail);
        }

    }
    private void logoutUser() {
        mAuth.signOut();
        // Redirect to LoginActivity after logout
        startActivity(new Intent(AdminProfileActivity.this, AdminMain.class));
        finish();
    }
    @Override
    public void onBackPressed() {
        startActivity(new Intent(AdminProfileActivity.this, AdminMain.class));
        finish();
        super.onBackPressed();
    }
}
