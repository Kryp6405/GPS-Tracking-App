package com.example.gpsapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.example.gpsapp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    //API KEY: AIzaSyCHrAqtn1fRlnkFTtGZISWXUN7IeA84g3Y

    //Variables for GoogleMap access
    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    //Variables for UI aspects inside InfoWindow at some location
    ImageView imgOfLoc;
    TextView address, latLong, distanceTraveled, elapsedTime, favoriteLocation;

    //Variables that will never be changed (do not need to be variables though)
    final int MY_PERMISSION_FINE_LOCATION = 1;
    final int MIN_TIME = 1000;
    final int MIN_DIST = 1;

    //Variables to access different aspects of a location
    LocationManager locationManager;
    LocationListener locationListener;
    Geocoder geocoder;

    //Variables for lists that need to be tracked at different locations
    List<Address> addresses;
    ArrayList<String> addyList;
    ArrayList<Location> locations;
    ArrayList<Long> times;

    //Variables that contain values of aspects at different locations
    String currentAddyS, favoriteLocationS;
    float distanceTraveledS;
    double latitudeS, longitudeS;
    float distTrav = 0;
    long max, timeSpent;
    long startTime = (int) (System.currentTimeMillis()/1000);
    LatLng latLon;

    //Variables for saving data throughout android lifecycle
    String cA, fL;
    float dT;
    double la, lo;
    long tS;

    //SaveInstanceState to store values on info window throughout lifecycle
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("LA", latitudeS);
        outState.putDouble("LO", longitudeS);
        outState.putString("CA", currentAddyS);
        outState.putFloat("DT", distanceTraveledS);
        outState.putLong("TS", timeSpent);
        outState.putString("FL", favoriteLocationS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //If we did changed from portrait to landscape, save the last seen values that
        //were on the info window to be outputted in new orientation
        if (savedInstanceState != null) {
            lo = savedInstanceState.getDouble("LO");
            la = savedInstanceState.getDouble("LA");
            cA = savedInstanceState.getString("CA");
            dT = savedInstanceState.getFloat("DT");
            tS = savedInstanceState.getLong("TS");
            fL = savedInstanceState.getString("FL");
        }

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Initialize lists to keep track of
        addresses =  new ArrayList<>();
        locations = new ArrayList<>();
        times = new ArrayList<>();
        addyList = new ArrayList<>();

        //Initializing the classes needed to get location information
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        geocoder = new Geocoder(this, Locale.US);

        //Method via listener that gives updates from location to location
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                try {
                    //Keep track of locations
                    locations.add(location);

                    //A temporary variable (to be reset every time method is called) that gets the current time
                    //after you have changed locations
                    long newTime = (int) (System.currentTimeMillis()/1000);

                    //Subtract the current time at the last location by the initial moment of time at the location and save into "main" dummy variable
                    //Then, keep track of all the times spent at locations
                    timeSpent = newTime - startTime;
                    times.add(timeSpent);

                    //Reset the starting time with current time so the relative time frame does not change
                    startTime = newTime;

                    //If we have more than one location, calculate`1234 the cumulative distance traveled
                    //Save this value into "main" dummy variable for lifecycle
                    if (locations.size() > 1) {
                        distTrav += location.distanceTo(locations.get(locations.size() - 2));
                        distanceTraveledS = (float) (Math.round(distTrav * 1000.0)/1000.0);
                    }

                    //Get latitude and longitude of the location
                    //Save these values into "main" dummy variables for lifcycle
                    latLon = new LatLng(location.getLatitude(), location.getLongitude());
                    latitudeS = Math.round(location.getLatitude()*1000.0)/1000.0;
                    longitudeS = Math.round(location.getLongitude()*1000.0)/1000.0;
                }
                catch (SecurityException e) {
                    e.printStackTrace();
                }

                try {
                    //Using geocoder class to obtain address of location given latitude and longitude parameters
                    //Then, keep track of all addresses at locations
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    currentAddyS = addresses.get(0).getAddressLine(0);
                    addyList.add(currentAddyS);
                }
                catch(IOException e){
                    e.printStackTrace();
                }

                //Creating marker option to enhance output view
                MarkerOptions markerOpt = new MarkerOptions();
                markerOpt.position(new LatLng(location.getLatitude(), location.getLongitude()));

                //(Analogous to a custom adapter) Outputs information of a location on a window that isn't the main xml
                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public View getInfoWindow(Marker marker) {
                        //Custom xml layout for output window
                        View v = getLayoutInflater().inflate(R.layout.windowlayout, null);
                        v.setBackgroundResource(R.drawable.edge);

                        //Initialize UI aspects in the output window
                        imgOfLoc = (ImageView) v.findViewById(R.id.imageView);
                        address = (TextView) v.findViewById(R.id.address);
                        latLong = (TextView) v.findViewById(R.id.latLong);
                        distanceTraveled = (TextView) v.findViewById(R.id.distanceTraveled);
                        elapsedTime = (TextView) v.findViewById(R.id.elapsedTime);
                        favoriteLocation = (TextView) v.findViewById(R.id.favoriteLocation);

                        //Checking for change from portrait to landscape
                        if(cA != null){
                            //If so, save the data and store into main dummy variables
                            imgOfLoc.setImageResource(R.drawable.park2);
                            address.setText(cA);
                            latLong.setText("(" + la + ", " + lo + ")");
                            distanceTraveled.setText(dT + " m");
                            elapsedTime.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", tS/3600, (tS%3600) / 60, tS%60));
                            favoriteLocation.setText(fL);

                            //Maintain total distance traveled
                            distTrav = dT;

                            //Reset all dummy variables needed for lifecycle
                            la = 0 ;
                            lo = 0;
                            cA = null;
                            dT = 0;
                            tS = 0;
                            fL = null;
                        }

                        else {
                            //Else just output "main" dummy variables
                            imgOfLoc.setImageResource(R.drawable.park2);
                            address.setText(currentAddyS);
                            latLong.setText("(" + latitudeS + ", " + longitudeS + ")");
                            distanceTraveled.setText(distanceTraveledS + " m");

                            //If theres more than one saved address...
                            if (addyList.size() > 1) {
                                //Find the fav location by comparing the most time spent at the location by looping through both lists
                                elapsedTime.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", timeSpent / 3600, (timeSpent % 3600) / 60, timeSpent % 60));
                                max = 0;
                                for (int i = 1; i < times.size(); i++) {
                                    if (times.get(i) > max) {
                                        max = times.get(i);
                                        favoriteLocationS = "Favorite Location:\n " + addyList.get(i - 1) + ". \nYou stayed for: " + String.format(Locale.getDefault(), "%d:%02d:%02d", times.get(i) / 3600, (times.get(i) % 3600) / 60, times.get(i) % 60);
                                        favoriteLocation.setText(favoriteLocationS);
                                    }
                                }
                            }
                        }

                        return v;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        return null;
                    }

                    @NonNull
                    private GoogleMap.InfoWindowAdapter getInfoWindowAdapter() {
                        return this;
                    }
                });

                //Add the marker on the map with the customized window
                //Move the camera to the marker everytime location changes
                mMap.addMarker(markerOpt).showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLon));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras){

            }

            @Override
            public void onProviderEnabled(@NonNull String provider){

            }

            @Override
            public void onProviderDisabled(@NonNull String provider){

            }
        };

        //Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String [] {Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_FINE_LOCATION);
        }

        //Request location
        else if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
        }
    }

    //Deregister listener
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    //Re-register listener
    /*
    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String [] {Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_FINE_LOCATION);
        }

        else if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,MIN_TIME,MIN_DIST,locationListener);
        }
    }
    */
}