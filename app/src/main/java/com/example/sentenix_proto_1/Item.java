package com.example.sentenix_proto_1;

public class Item {
    private String key;
    private String description;
    private String location;
    private String time;

    public Item() {
        // Default constructor required for Firebase
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    // Additional methods for getting details and name
    public String getDetails() {
        return "Description: " + description + "\nLocation: " + location + "\nTime: " + time;
    }

    public String getName() {
        return "Case ID: " + key;
    }
}
