package com.example.sentenix_proto_1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;




import androidx.appcompat.app.AppCompatActivity;

import com.google.common.net.InternetDomainName;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

public class ProfileActivity extends AppCompatActivity {
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

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
    @Override
    public void onBackPressed() {
        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        finish();
        super.onBackPressed();
    }
}
