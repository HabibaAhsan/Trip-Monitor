package com.example.tripsapp;

import java.io.Serializable;

public class Violation implements Serializable {
    private String type;
    private String timestamp;
    private String location;

    public Violation(String type, String timestamp, String location) {
        this.type = type;
        this.timestamp = timestamp;
        this.location = location;
    }

    public String getType() { return type; }
    public String getTimestamp() { return timestamp; }
    public String getLocation() { return location; }
}
