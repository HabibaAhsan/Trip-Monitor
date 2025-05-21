package com.example.tripsapp;

import java.io.Serializable;
import java.util.ArrayList;

public class Trip implements Serializable {
    private String tripNumber;
    private String date;
    private ArrayList<Violation> violations;

    public Trip(String tripNumber, String date) {
        this.tripNumber = tripNumber;
        this.date = date;
        this.violations = new ArrayList<>();
    }

    public String getTripNumber() { return tripNumber; }
    public String getDate() { return date; }
    public ArrayList<Violation> getViolations() { return violations; }

    public void addViolation(Violation violation) {
        violations.add(violation);
    }
}
