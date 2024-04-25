package com.example.sentenix_proto_1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int SMS_PERMISSION_REQUEST_CODE = 1002;

    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private Button mLoginButton;
    private Button mRegisterLinkButton;

    private FirebaseAuth mAuth;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();


        mEmailEditText = findViewById(R.id.email_edit_text);
        mPasswordEditText = findViewById(R.id.password_edit_text);
        mLoginButton = findViewById(R.id.login_button);
        mRegisterLinkButton = findViewById(R.id.register_link_button);
        mRegisterLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToRegister();
            }

            private void navigateToRegister() {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish(); // Optional: Finish the current activity to prevent going back to it
            }
        });
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmailEditText.getText().toString().trim();
                String password = mPasswordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    attemptLogin(email, password);
                }
            }
        });

        // Check and request permissions
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Location permission granted, now request SMS permission
            checkSmsPermission();
        }
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
        } else {
            // Both location and SMS permissions granted, enable login button
            mLoginButton.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission granted, now request SMS permission
            checkSmsPermission();
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Both location and SMS permissions granted, enable login button
            mLoginButton.setEnabled(true);
        }
    }

    private void attemptLogin(String email, String password) {
        if (email.contains("@")) {
            // Email and password login
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseAuth auth = FirebaseAuth.getInstance();
                                FirebaseUser currentUser = auth.getCurrentUser();
                                String uid = currentUser.getUid();
                                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
                                userRef.child("isAD").get().addOnCompleteListener(task2 -> {
                                    if(task2.isSuccessful()) {
                                        DataSnapshot dataSnapshot = task2.getResult();
                                        if(dataSnapshot.exists()) {
                                            int isAD = dataSnapshot.getValue(Integer.class);
                                            if(isAD==1) {
                                                Toast.makeText(LoginActivity.this, "Logging in as Admin", Toast.LENGTH_LONG).show();
                                                Intent intent = new Intent(LoginActivity.this, AdminMain.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                            else if(isAD==2) {
                                                Toast.makeText(LoginActivity.this, "Logging in as Police", Toast.LENGTH_LONG).show();
                                                Intent intent = new Intent(LoginActivity.this, PoliceMain.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                            else{
                                                Toast.makeText(LoginActivity.this, "Login as User", Toast.LENGTH_LONG).show();
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        }
                                    }
                                });



                                // Login success, navigate to MainActivity
//                                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
//                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//                                startActivity(intent);
//                                finish(); // Close LoginActivity to prevent returning to it using the back button
                            } else {
                                Toast.makeText(LoginActivity.this, "Authentication failed. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            // Phone number authentication
            String phoneNumber = email; // Assuming phone number is provided in the email field
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,
                    60,
                    TimeUnit.SECONDS,
                    this,
                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                            mAuth.signInWithCredential(phoneAuthCredential)
                                    .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                // Phone authentication success, navigate to MainActivity
                                                Toast.makeText(LoginActivity.this, "Phone number authentication successful", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish(); // Close LoginActivity
                                            } else {
                                                Toast.makeText(LoginActivity.this, "Phone authentication failed. Please try again.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            Toast.makeText(LoginActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
