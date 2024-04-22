package com.example.sentenix_proto_1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Demoactivity extends AppCompatActivity {
    private static final String TAG = "Demoactivity";

    private ListView listView;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_reports);

        listView = findViewById(R.id.list_view);
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(this, itemList);
        listView.setAdapter(itemAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("reports");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
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
                Toast.makeText(Demoactivity.this, "Failed to load items.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onBackPressed() {
        startActivity(new Intent(Demoactivity.this, MainActivity.class));
        finish();
        super.onBackPressed();
    }
}
