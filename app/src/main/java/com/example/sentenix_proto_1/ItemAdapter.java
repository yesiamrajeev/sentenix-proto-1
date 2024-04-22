package com.example.sentenix_proto_1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ItemAdapter extends ArrayAdapter<Item> {
    private Context context;
    private List<Item> itemList;
    private DatabaseReference databaseReference;

    public ItemAdapter(Context context, List<Item> itemList) {
        super(context, 0, itemList);
        this.context = context;
        this.itemList = itemList;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("reports");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_row, parent, false);
        }

        final Item currentItem = itemList.get(position);

        TextView itemDetails = view.findViewById(R.id.item_details);
        itemDetails.setText("Case Details: " + currentItem.getDetails());

        TextView itemText = view.findViewById(R.id.item_text);
        itemText.setText(currentItem.getName());

        Button deleteButton = view.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteItem(currentItem);
            }
        });

        return view;
    }

    private void deleteItem(Item item) {
        String itemKey = item.getKey();
        if (itemKey != null && !itemKey.isEmpty()) {
            databaseReference.child(itemKey).removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(context, "Node deleted successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Failed to delete node", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(context, "Item key is null or empty", Toast.LENGTH_SHORT).show();
        }
    }
}
