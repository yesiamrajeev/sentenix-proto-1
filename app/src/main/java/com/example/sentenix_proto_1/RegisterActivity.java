package com.example.sentenix_proto_1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {
    private EditText mUsernameEditText;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mPhoneEditText;
    private EditText mOtpEditText;
    private Button mRegisterButton;
    private Button mSendOtpButton;
    private Button mVerifyOtpButton;
    private Button mVerifyOtpButton2;


    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        mUsernameEditText = findViewById(R.id.username_edit_text);
        mEmailEditText = findViewById(R.id.email_edit_text);
        mPasswordEditText = findViewById(R.id.password_edit_text);
        mPhoneEditText = findViewById(R.id.phone_edit_text);
        mOtpEditText = findViewById(R.id.otp_edit_text);
        mRegisterButton = findViewById(R.id.register_button);
        //mSendOtpButton = findViewById(R.id.send_otp_button);
        mVerifyOtpButton = findViewById(R.id.verify_otp_button);
        mVerifyOtpButton2 = findViewById(R.id.login_link_button);

//        mSendOtpButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String phoneNumber = mPhoneEditText.getText().toString().trim();
//                if (!TextUtils.isEmpty(phoneNumber)) {
//                    sendOtp(phoneNumber);
//                } else {
//                    Toast.makeText(RegisterActivity.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
        mVerifyOtpButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin();
            }
        });
        mVerifyOtpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String otp = mOtpEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(otp) && !TextUtils.isEmpty(mVerificationId)) {
                    verifyOtp(mVerificationId, otp);
                } else {
                    Toast.makeText(RegisterActivity.this, "Please enter OTP and verify", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = mUsernameEditText.getText().toString().trim();
                String email = mEmailEditText.getText().toString().trim();
                String password = mPasswordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    registerUser(username, email, password);
                }
            }
        });
    }

    private void sendOtp(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        // Auto-retrieval of SMS completed by Google
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(RegisterActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verificationId, forceResendingToken);
                        mVerificationId = verificationId;
                        mResendToken = forceResendingToken;
                        Toast.makeText(RegisterActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void verifyOtp(String verificationId, String otp) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            if (currentUser != null) {
                                String username = mUsernameEditText.getText().toString().trim();
                                String email = mEmailEditText.getText().toString().trim();
                                String password = mPasswordEditText.getText().toString().trim();
                                registerUser(username, email, password);
                            }
                        } else {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(RegisterActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                            } else if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                Toast.makeText(RegisterActivity.this, "Invalid user", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RegisterActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void registerUser(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            if (currentUser != null) {
                                // Get the unique user ID generated by Firebase Authentication
                                String userId = currentUser.getUid();

                                // Create a User object with only username and phone number
                                User user = new User(username, mPhoneEditText.getText().toString().trim(), 0.0, 0.0,0);

                                // Store the user object in the Realtime Database under the user's unique ID
                                mDatabase.child(userId).setValue(user)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Update display name for the user
                                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                            .setDisplayName(username)
                                                            .build();
                                                    currentUser.updateProfile(profileUpdates)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                                                        navigateToLogin();
                                                                    } else {
                                                                        Toast.makeText(RegisterActivity.this, "Failed to set display name", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            });
                                                } else {
                                                    Toast.makeText(RegisterActivity.this, "Failed to upload user data", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }



    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);

        startActivity(intent);
        finish();
    }
}
