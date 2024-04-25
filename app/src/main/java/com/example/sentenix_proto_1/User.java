package com.example.sentenix_proto_1;

public class User {
    private String username;
    private String phoneNumber;
    private double latitude;
    private double longitude;

    private int isAD;


    public User() {
        // Default constructor required for Firebase
    }

    public User(String username, String phoneNumber, double latitude, double longitude, int isAD) {
        this.username = username;
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isAD = isAD;
    }

    public String getUsername() {
        return username;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getisAD() { return isAD;}
}
