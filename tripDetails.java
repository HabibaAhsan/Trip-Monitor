package com.example.tripsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class tripDetails extends AppCompatActivity {
    private TextView tripNoText;
    private TextView tripDateText;
    private LinearLayout violationListContainer;
    private ArrayList<Trip> allTrips;
    private static final String FILE_NAME = "trips_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trip_details);

        tripNoText = findViewById(R.id.trip_no);
        tripDateText = findViewById(R.id.trip_date);
        violationListContainer = findViewById(R.id.violationListContainer);
        Button backButton = findViewById(R.id.back_btn);

        // Load trips data from storage
        allTrips = loadTrips();

        // Get the trip index from intent
        int tripIndex = getIntent().getIntExtra("tripIndex", -1);

        // Display trip details if the index is valid
        if (tripIndex >= 0 && tripIndex < allTrips.size()) {
            Trip selectedTrip = allTrips.get(tripIndex);
            displayTripDetails(selectedTrip);
        }

        // Set back button functionality
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void displayTripDetails(Trip trip) {
        // Set trip number and date
        tripNoText.setText("Trip " + trip.getTripNumber());
        tripDateText.setText(trip.getDate());

        // Populate the list of violations
        for (Violation violation : trip.getViolations()) {
            // Create a new layout to hold the violation and the button
            LinearLayout violationLayout = new LinearLayout(this);
            violationLayout.setOrientation(LinearLayout.HORIZONTAL);
            violationLayout.setPadding(5, 10, 5, 10);

            // Create the TextView for the violation details
            TextView violationText = new TextView(this);
            violationText.setText(violation.getType() + " at " + violation.getTimestamp() + " - " + violation.getLocation());
            violationText.setTextColor(getResources().getColor(R.color.text_color, null));
            violationText.setTextSize(16);
            violationLayout.addView(violationText);

            // Create the "View on Map" button
            Button viewMapButton = new Button(this);
            viewMapButton.setText("View Map");
            viewMapButton.setBackgroundTintList(getResources().getColorStateList(R.color.trip_button_color, null));
            viewMapButton.setTextColor(getResources().getColor(R.color.trip_button_text_color, null));
            viewMapButton.setPadding(20, 20, 20, 20);
            viewMapButton.setOnClickListener(v -> openMap(violation.getLocation()));

            // Add the violation layout to the container
            violationListContainer.addView(violationLayout);
            violationListContainer.addView(viewMapButton);
        }
    }
    private void openMap(String location) {
        // Example location format: "Lat: 12.9716, Long: 77.5946"
        String[] parts = location.split(", ");
        if (parts.length == 2) {
            String latitudeStr = parts[0].replace("Lat: ", "");
            String longitudeStr = parts[1].replace("Long: ", "");

            try {
                double latitude = Double.parseDouble(latitudeStr);
                double longitude = Double.parseDouble(longitudeStr);

                // Construct a URI to launch Google Maps
                Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);

            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }


    private ArrayList<Trip> loadTrips() {
        ArrayList<Trip> trips = new ArrayList<>();
        try (FileInputStream fis = openFileInput(FILE_NAME);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            trips = (ArrayList<Trip>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trips;
    }
}
