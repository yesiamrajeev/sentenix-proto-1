package com.example.sentenix_proto_1.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sentenix_proto_1.ItemPolice;
import com.example.sentenix_proto_1.ItemAdapter;
import com.example.sentenix_proto_1.ItemAdapterPolice;
import com.example.sentenix_proto_1.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PoliceNotificationFragment extends Fragment {
    private static final String TAG = "DemoFragment";

    private ListView listView;
    private ItemAdapterPolice itemAdapter;
    private List<ItemPolice> itemList;
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_police, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = view.findViewById(R.id.list_view_police);
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapterPolice(requireContext(), itemList);
        listView.setAdapter(itemAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("reports");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ItemPolice item = snapshot.getValue(ItemPolice.class);
                    Boolean isVerified = snapshot.child("verified").getValue(Boolean.class);
                    if (item != null && Boolean.TRUE.equals(isVerified)) {
                        item.setKey(snapshot.getKey()); // Set the key for the item
                        itemList.add(item);
                    }
                }
                itemAdapter.notifyDataSetChanged();
                Log.d(TAG, "Item List Size: " + itemList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database Error: " + databaseError.getMessage());
                Toast.makeText(requireContext(), "Failed to load items.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
