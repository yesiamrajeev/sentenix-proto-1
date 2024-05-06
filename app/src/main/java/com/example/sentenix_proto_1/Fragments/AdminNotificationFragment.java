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

import com.example.sentenix_proto_1.Item;
import com.example.sentenix_proto_1.ItemAdapter;
import com.example.sentenix_proto_1.R;
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

public class AdminNotificationFragment extends Fragment {
    private static final String TAG = "DemoFragment";

    private ListView listView;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = view.findViewById(R.id.list_view);
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(requireContext(), itemList);
        listView.setAdapter(itemAdapter);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("reports");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    String AdminName = item.getAdminName();
                    if (item != null ) {
                        if((Objects.equals(AdminName, "Not Allotted"))){
                            item.setKey(snapshot.getKey()); // Set the key for the item
                            itemList.add(item);
                        }

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
