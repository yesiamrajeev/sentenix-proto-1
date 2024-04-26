package com.example.sentenix_proto_1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Objects;

public class ItemAdapter extends ArrayAdapter<Item> {
    private Context context;
    private List<Item> itemList;

    private DatabaseReference databaseReference;

    int who;

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


        Switch verifiedSwitch = view.findViewById(R.id.verifiedSwitch);
        Switch officerSwitch = view.findViewById(R.id.officerSwitch);


        String itemKey = currentItem.getKey();
        DatabaseReference itemRef = databaseReference.child(itemKey);


        itemRef.child("verified").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    boolean verifiedValue = dataSnapshot.getValue(Boolean.class);
                    verifiedSwitch.setChecked(verifiedValue);
                }
            }
        });
        itemRef.child("officerAssigned").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    boolean officerAssignedValue = dataSnapshot.getValue(Boolean.class);
                    officerSwitch.setChecked(officerAssignedValue);
                }
            }
        });








        verifiedSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (verifiedSwitch.isChecked()) {
                    updateProgress(currentItem, 1,"vs"); // Call updateProgress with i = 1 when Switch is checked
                }
                else{
                    updateProgress(currentItem, -1,"vs");
                }
            }
        });

        officerSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (officerSwitch.isChecked()) {
                    updateProgress(currentItem, 1,"oa"); // Call updateProgress with i = 1 when Switch is checked
                }
                else{
                    updateProgress(currentItem, -1,"oa");
                }
            }
        });




        Button rejectButton = view.findViewById(R.id.reject_button);
        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteItem(currentItem);
            }
        });



        Button acceptCaseBtn = view.findViewById(R.id.accept_button); //AcceptButton for accepting the case
        acceptCaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptCase(currentItem);
            }
        });










        return view;
    }

    private void updateProgress(Item item, int i, String who) {
            String itemKey = item.getKey();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentAmin = auth.getCurrentUser();
            String adminName = user.getDisplayName();

            if (itemKey != null && !itemKey.isEmpty()) {

                DatabaseReference itemRef = databaseReference.child(itemKey); //// reference to the current item in the database

                itemRef.child("progress").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String progressValue = dataSnapshot.getValue(String.class);
                        if (progressValue != null) {
                            int progressInt = Integer.parseInt(progressValue.replace("%", "")); //getting the initial value from database
                            int progressIncrement = 0;//for temporary calculation

                            if(i>0 && Objects.equals(who, "vs")){
                                progressIncrement = 25;
                                itemRef.child("verified").setValue(true);

                            }
                            else if(i<0 && Objects.equals(who, "vs")){
                                progressIncrement = -25;
                                itemRef.child("verified").setValue(false);
                            }
                            else if(i>0 && Objects.equals(who, "oa")){
                                progressIncrement = 25;
                                itemRef.child("officerAssigned").setValue(true);
                            }
                            else if(i<0 && Objects.equals(who, "oa")){
                                progressIncrement = -25;
                                itemRef.child("officerAssigned").setValue(false);
                            }

                            int newProgressValue = progressInt + progressIncrement;
                            String progressFinal = newProgressValue + "%";
                            itemRef.child("progress").setValue(progressFinal);


//                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                        @Override
//                                        public void onSuccess(Void aVoid) {
//                                            Toast.makeText(context, "Update: Verified", Toast.LENGTH_SHORT).show();
//                                        }
//                                    })
//                                    .addOnFailureListener(new OnFailureListener() {
//                                        @Override
//                                        public void onFailure(@NonNull Exception e) {
//                                            Toast.makeText(context, "Not Updated", Toast.LENGTH_SHORT).show();
//                                        }
//                                    });
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Handle errors here
                }
            });


        }
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
    private void acceptCase(Item item) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentAmin = auth.getCurrentUser();
        String adminName = user.getDisplayName();
        String itemKey = item.getKey();


        String uid =user.getUid();
        DatabaseReference uref = FirebaseDatabase.getInstance().getReference("users").child(uid);
        uref.child("isAD").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) { who = dataSnapshot.getValue(Integer.class);}});

        if (itemKey != null && !itemKey.isEmpty()) {
            // Get the reference to the specific item in the database
            DatabaseReference itemRef = databaseReference.child(itemKey);

            // Update the name key with the new name
            if(who==1) {
                itemRef.child("adminName").setValue(adminName).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(context, "Name updated successfully", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Failed to update name", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
            else if (who==2) {
                itemRef.child("policeName").setValue(adminName).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(context, "Name updated successfully", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Failed to update name", Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        }
        else {
                Toast.makeText(context, "Item key is null or empty", Toast.LENGTH_SHORT).show();
        }


    }
}
