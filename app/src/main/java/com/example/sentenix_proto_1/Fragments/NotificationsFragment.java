package com.example.sentenix_proto_1.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sentenix_proto_1.R;
import com.example.sentenix_proto_1.Report;
import com.example.sentenix_proto_1.ReportsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotificationsFragment extends Fragment {

    private RecyclerView notificationRecyclerView;
    private ReportsAdapter reportsAdapter;
    private DatabaseReference reportsRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize RecyclerView
        notificationRecyclerView = view.findViewById(R.id.notificationRecyclerView);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        reportsAdapter = new ReportsAdapter();
        notificationRecyclerView.setAdapter(reportsAdapter);

        // Initialize Firebase Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        reportsRef = database.getReference("reports"); // reportsRef variable storing Reference to (reports node) in database


        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        assert user != null;
        String currentUserID = user.getUid();


        // Listen for any data changes in the database
        //ondata change it will simply add any changes to reportlist then finally use reportAdapter to show the data in recyclerview
        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Report> reportList = new ArrayList<>();
                // Iterate through each report
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Retrieve report data
                    Report report = snapshot.getValue(Report.class);
                    String caseUserID = report.getUserID();
                    if (report != null && Objects.equals(caseUserID, currentUserID)) { // here 2 objects's values are compared - so that only current user's data is fetched from report database
                        reportList.add(report);
                    }
                }
                // Update RecyclerView data
                reportsAdapter.setData(reportList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors
            }
        });
    }
}