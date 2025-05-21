package com.example.tripsapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class homeScreen extends AppCompatActivity implements SensorEventListener, LocationListener {
    private Button startButton, stopButton, tripsButton;
    private TextView tripDetailsText, tripNum;
    private SensorManager sensorManager;
    private LocationManager locationManager;
    private ArrayList<Trip> allTrips;
    private Trip currentTrip;
    private String tripNumber;
    private Location currentLocation;
    private static final String FILE_NAME = "trips_data";
    private static final float HARSH_BRAKE_THRESHOLD = -1.0f;
    private static final float STRONG_ACCELERATION_THRESHOLD = 20.0f;
    private static final float HARSH_TURN_THRESHOLD = 4.0f;
    private static final float OVER_SPEED_THRESHOLD = 30.0f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);

        // Check location permissions
        if (!hasLocationPermission()) {
            requestLocationPermission();
        }

        initializeComponents();
    }

    private void initializeComponents() {
        startButton = findViewById(R.id.start_btn);
        stopButton = findViewById(R.id.stop_btn);
        tripsButton = findViewById(R.id.trips_btn);
        tripDetailsText = findViewById(R.id.trip_details);
        tripNum = findViewById(R.id.trip_no);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        allTrips = loadTrips();
        //allTrips.clear();
        tripNumber = String.valueOf(allTrips.size() + 1);
        tripNum.setText("Trip " + tripNumber);
        startButton.setOnClickListener(v -> startTrip());
        stopButton.setOnClickListener(v -> stopTrip());
        tripsButton.setOnClickListener(v -> {
            Intent intent = new Intent(homeScreen.this, tripsScreen.class);
            startActivity(intent);
        });
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void startTrip() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        tripNumber = String.valueOf(allTrips.size() + 1);
        tripNum.setText("Trip " + tripNumber);
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        currentTrip = new Trip(tripNumber, date);

        // Register accelerometer and gyroscope sensors
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);

        // Check if GPS is enabled
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Request location updates from GPS provider
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            tripDetailsText.setText("GPS is not enabled.");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
        }

        tripDetailsText.setText("Trip started. Recording violations...");
    }


    private void stopTrip() {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);

        if (currentTrip != null) {
            allTrips.add(currentTrip);
            saveTrips(allTrips);
            tripDetailsText.append("\nTrip ended. Violations recorded: " + currentTrip.getViolations().size());
            currentTrip = null;
        }
    }

    private void recordViolation(String type, float speed, Location location) {
        String timestamp = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
        String violationInfo;

        switch (type) {
            case "Strong Acceleration":
                violationInfo = "Harsh Accelerate";
                break;
            case "Harsh Braking":
                violationInfo = "Harsh Brake";
                break;
            case "Harsh Turning":
                violationInfo = "Harsh Turn";
                break;
            case "Over Speeding":
                violationInfo = String.format("Over speed %.0f kmph", speed);
                break;
            default:
                violationInfo = type;
        }

        String locationDetails;
        if (location != null) {
            locationDetails = getCurrentLocation(location);
        } else {
            locationDetails = "Location unavailable";
        }
        currentTrip.addViolation(new Violation(type, timestamp, locationDetails));

        String violationDetails = String.format("%s – %s", timestamp, violationInfo);
        tripDetailsText.append("\n" + violationDetails);
    }

    private String getCurrentLocation(Location location) {
        if(location == null){
            Log.d("Location", "Location is null in get LOC");
        }
        Log.d("Location", "Location updated: Lat=" + location.getLatitude() + " Long=" + location.getLongitude());
        return String.format(Locale.getDefault(), "Lat: %.4f, Long: %.4f", location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (location != null) {
            currentLocation = location;  // Store the current location
            float speed = location.getSpeed() * 3.6f; // Convert m/s to km/h
            if (speed > OVER_SPEED_THRESHOLD) {
                recordViolation("Over Speeding", speed, location);
            }
        } else {
            Log.d("Location", "Location is null in onLocationChanged");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (currentTrip == null) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double acceleration = Math.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
            if (acceleration > STRONG_ACCELERATION_THRESHOLD) {
                if (currentLocation != null) {
                    recordViolation("Strong Acceleration", 0, currentLocation); // Pass the current location
                }
            } else if (acceleration < HARSH_BRAKE_THRESHOLD) {
                if (currentLocation != null) {
                    recordViolation("Harsh Braking", 0, currentLocation); // Pass the current location
                }
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            double rotationRate = Math.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
            if (rotationRate > HARSH_TURN_THRESHOLD) {
                if (currentLocation != null) {
                    recordViolation("Harsh Turning", 0, currentLocation); // Pass the current location
                }
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTrip();
            } else {
                tripDetailsText.setText("Location permission is required to record trip details.");
            }
        }
    }

    private void saveTrips(ArrayList<Trip> trips) {
        try (FileOutputStream fos = openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(trips);
        } catch (Exception e) {
            e.printStackTrace();
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
