// Report.java
package com.example.sentenix_proto_1;

public class Report {
    private String description;
    private String location;
    private String time; // Add time field

    public Report() {
        // Default constructor required for Firebase
    }

    public Report(String description, String location, String time) {
        this.description = "D.s: "+description;
        this.location = "Location: "+location;
        this.time = "Time: "+time;
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
}
