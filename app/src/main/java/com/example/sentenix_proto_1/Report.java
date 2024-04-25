// Report.java
package com.example.sentenix_proto_1;

public class Report {
    private String description;
    private String location;
    private String time;
    private String date;

    private String userID;

    private String adminName = "Not Allotted";
    private String policeName = "Not Allotted";
    private String progress = "0";
    private boolean verified = false;
    private boolean officerAssigned = false;
    private boolean inProgress = false;
    private boolean Closed = false;


    public Report() {
        // Default constructor required for Firebase
    }

    public Report(String description, String location, String time, String date, String userID) {
        this.description = description;
        this.location = location;
        this.time = time;
        this.date = date;
        this.userID = userID;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUserID() {
        return userID;
    }

    public String getProgress() { // case progress
        return progress;
    }

    public String getPoliceName() { //police allocated
        return policeName;
    }

    public String getAdminName() { //admin allocated
        return adminName;
    }



    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean officerAssigned) {
        this.officerAssigned = officerAssigned;
    }



    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean officerAssigned) {
        this.officerAssigned = officerAssigned;
    }

    public boolean isClosed() {
        return Closed;
    }

    public boolean isOfficerAssigned() {
        return officerAssigned;
    }

    public void setOfficerAssigned(boolean officerAssigned) {
        this.officerAssigned = officerAssigned;
    }
}
