package com.example.sentenix_proto_1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CaseDetailAdmin extends AppCompatActivity {
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_detail_admin);

        Intent intent = getIntent();
        String itemKey = intent.getStringExtra("itemKey");
        DatabaseReference caseRef = databaseReference.child("reports").child(itemKey);



        Button editButton = findViewById(R.id.edit_btn);
        Button updateButton = findViewById(R.id.update_btn);
        EditText caseDetails = findViewById(R.id.case_edit_text);
        TextView caseID = findViewById(R.id.case_id_tv);
        TextView userID = findViewById(R.id.user_tv);
        TextView dateTv = findViewById(R.id.date_tv);
        TextView locationTv = findViewById(R.id.location_tv);


        caseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                caseID.setText("Case ID: "+itemKey);
                locationTv.setText("Location: "+snapshot.child("location").getValue().toString());
                dateTv.setText("Date: "+snapshot.child("date").getValue().toString());
                userID.setText("User ID: "+snapshot.child("userID").getValue().toString());

                String details = snapshot.child("details").getValue(String.class);
                if(details !=null) {
                    caseDetails.setText(snapshot.child("details").getValue().toString());
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });








        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isEditable = !caseDetails.isEnabled();
                caseDetails.setEnabled(isEditable);
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                caseDetails.getText().toString();

                caseRef.child("details").setValue(caseDetails.getText().toString());
            }
        });


    }


}
