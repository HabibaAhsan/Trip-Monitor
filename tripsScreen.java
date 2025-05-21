package com.example.tripsapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class tripsScreen extends AppCompatActivity {
    private LinearLayout tripListContainer;
    private ArrayList<Trip> allTrips;
    private static final String FILE_NAME = "trips_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trips_screen);
        Button backButton = findViewById(R.id.back_btn);
        tripListContainer = findViewById(R.id.tripListContainer);

        allTrips = loadTrips();
        displayTrips();
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void displayTrips() {
        for (int i = 0; i < allTrips.size(); i++) {
            Trip trip = allTrips.get(i);
            Button tripButton = new Button(this);
            tripButton.setText("Trip " + trip.getTripNumber() + " – " + trip.getDate());
            tripButton.setBackgroundTintList(getResources().getColorStateList(R.color.trip_button_color, null));
            tripButton.setTextColor(getResources().getColor(R.color.trip_button_text_color, null));
            tripButton.setPadding(10, 20, 10, 20);

            final int index = i; // For identifying the specific trip in the OnClickListener
            tripButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openTripDetailsScreen(index);
                }
            });

            tripListContainer.addView(tripButton);
        }
    }

    private void openTripDetailsScreen(int tripIndex) {
        Intent intent = new Intent(tripsScreen.this, tripDetails.class);
        intent.putExtra("tripIndex", tripIndex);
        startActivity(intent);
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
