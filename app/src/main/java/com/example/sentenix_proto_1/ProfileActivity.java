package com.example.sentenix_proto_1;

import android.content.Intent;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

    }
    @Override
    public void onBackPressed() {
        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        finish();
        super.onBackPressed();
    }
}
