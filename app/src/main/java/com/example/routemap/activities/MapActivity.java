package com.example.routemap.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.routemap.R;
import com.example.routemap.domain.InfoMarker;
import com.example.routemap.domain.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.*;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapClickListener, OnMarkerClickListener, OnInfoWindowClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private Toolbar toolbar;
    private GoogleMap map;

    private View v1;
    private View v2;

    private List<Marker> markers;

    private SharedPreferences preferences;
    private WifiManager wifiManager;

    private Location location;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private boolean showCurrentLocation = false;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        createLocationRequest();

        markers = new ArrayList<>();

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startLocationUpdates();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String networkConnectionType = preferences.getString("networkType", "Wifi");
        if(networkConnectionType.equals("Wifi")) {
            wifiManager.setWifiEnabled(true);
        }
        else {
            wifiManager.setWifiEnabled(false);
        }
        showCurrentLocation = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.filterButton:

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                LayoutInflater inflater = getLayoutInflater();

                v2 = inflater.inflate(R.layout.filter_options, null);

                builder.setView(v2).setPositiveButton("Filtrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String type = ((Spinner) v2.findViewById(R.id.filterType)).getSelectedItem().toString();
                        String level = ((Spinner) v2.findViewById(R.id.filterLevel)).getSelectedItem().toString();
                        String author = ((TextView) v2.findViewById(R.id.filterAuthor)).getText().toString();

                        filterResults(type, level, author);
                    }
                }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
                return true;

            case R.id.settingsButton:
                Intent in = new Intent(this, PreferencesActivity.class);
                startActivity(in);
                return true;

            case R.id.logoutButton:
                firebaseAuth.signOut();
                Toast.makeText(this, "Sesion cerrada", Toast.LENGTH_SHORT).show();
                finish();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        //Init configuration
        map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        String mapType = preferences.getString("defaultTypeMap", "Normal");
        switch (mapType) {
            case "Normal":
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case "Satelite":
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case "Terreno":
                map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case "Hibrido":
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
        }

        map.setInfoWindowAdapter(new CustomInfoWindowAdapter(getLayoutInflater().from(this)));

        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnInfoWindowClickListener(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        addPersonalizedMarker(latLng);
    }

    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                location = locationResult.getLastLocation();
                if (location != null) {
                    if (!showCurrentLocation) {
                        addDefaultMarker("Accidente", "Marker predefinido 1", "Leve", new User("test@test.com", "Sistem", "1234"), new LatLng(location.getLatitude() + 0.001d, location.getLongitude()));
                        addDefaultMarker("Obras", "Marker predefinido 2", "Moderado", new User("test@test.com", "Sistem", "1234"), new LatLng(location.getLatitude(), location.getLongitude() + 0.001d));
                        addDefaultMarker("Zona Peatonal", "Marker predefinido 3", "Grave", new User("test@test.com", "Sistem", "1234"), new LatLng(location.getLatitude() + 0.002d, location.getLongitude()));
                        addDefaultMarker("Obras", "Marker predefinido 4", "Leve", new User("test@test.com", "Sistem", "1234"), new LatLng(location.getLatitude(), location.getLongitude() + 0.002d));
                        addDefaultMarker("Visibilidad", "Marker predefinido 5", "Moderado", new User("test@test.com", "Sistem", "1234"), new LatLng(location.getLatitude() + 0.002d, location.getLongitude() + 0.002d));

                        String zoomString = preferences.getString("defaultZoomMap", "16");
                        int zoom = Integer.parseInt(zoomString);

                        goToMyCurrentLocation(zoom);
                        showCurrentLocation = true;
                    }
                }
            }
        };
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(100000);
        locationRequest.setFastestInterval(50000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void goToMyCurrentLocation (int zoom){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (location != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 10));
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                        .zoom(zoom)
                        .build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            else {
                Toast.makeText(this, "Localización desconocida", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "Falta permiso para obtener la localización", Toast.LENGTH_SHORT).show();
        }
    }

    public void addDefaultMarker(String type, String description, String level, User author, LatLng latLng) {

        InfoMarker infoMarker = new InfoMarker();
        infoMarker.setType(type);
        infoMarker.setDescription(description);
        infoMarker.setLevel(level);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
        infoMarker.setDate(sdf.format(new Date()));

        //3º Entrega: Obtener de BBDD
        infoMarker.setAuthor(author);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);

        switch (infoMarker.getLevel()) {
            case "Leve":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                break;
            case "Moderado":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                break;
            case "Grave":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                break;
        }

        Marker marker = map.addMarker(markerOptions);
        marker.setTag(infoMarker);
        markers.add(marker);
    }

    public void addPersonalizedMarker(final LatLng latLng) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        v1 = inflater.inflate(R.layout.input_info_marker, null);

        builder.setView(v1).setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InfoMarker infoMarker = new InfoMarker();
                infoMarker.setType(((Spinner) v1.findViewById(R.id.input_info_type)).getSelectedItem().toString());
                infoMarker.setDescription(((TextView) v1.findViewById(R.id.input_info_description)).getText().toString());
                infoMarker.setLevel(((Spinner) v1.findViewById(R.id.input_info_level)).getSelectedItem().toString());

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
                infoMarker.setDate(sdf.format(new Date()));

                //3º Entrega: Obtener de BBDD
                infoMarker.setAuthor(new User("admin@admin.es", "admin", "admin"));

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);

                switch (infoMarker.getLevel()) {
                    case "Leve":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        break;
                    case "Moderado":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                        break;
                    case "Grave":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        break;
                }

                Marker marker = map.addMarker(markerOptions);
                marker.setTag(infoMarker);
                markers.add(marker);
            }
        }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    public void filterResults(String type, String level, String author) {
        if(!markers.isEmpty()) {
            for (Marker marker : markers) {
                InfoMarker infoMarker = (InfoMarker) marker.getTag();
                if ((infoMarker.getType().equals(type) || type.equals("Todos"))
                        && (infoMarker.getLevel().equals(level) || level.equals("Todos"))
                        && (infoMarker.getAuthor().getUser().equals(author) || author.isEmpty())) {
                            marker.setVisible(true);
                }
                else {
                    marker.setVisible(false);
                }
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return true;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        marker.hideInfoWindow();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("defaultTypeMap")) {
            String mapType = sharedPreferences.getString("defaultTypeMap", "Normal");
            switch (mapType) {
                case "Normal":
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    break;
                case "Satelite":
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    break;
                case "Terreno":
                    map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    break;
                case "Hibrido":
                    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    break;
            }
        }

        if(key.equals("defaultZoomMap")) {
            String zoomString = sharedPreferences.getString("defaultZoomMap", "16");
            int zoom =  Integer.parseInt(zoomString);
            map.animateCamera(CameraUpdateFactory.zoomTo(zoom));
        }

        if(key.equals("networkType")) {
            String networkConnectionType = sharedPreferences.getString("networkType", "Wifi");
            if(networkConnectionType.equals("Wifi")) {
                wifiManager.setWifiEnabled(true);
            }
            else {
                wifiManager.setWifiEnabled(false);
            }
        }
    }

    //Custom Info Window for the markers
    public static class CustomInfoWindowAdapter implements InfoWindowAdapter {

        private LayoutInflater inflater;

        CustomInfoWindowAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            View v = inflater.inflate(R.layout.info_window_marker, null);

            //Details of the marker
            InfoMarker infoMarker = (InfoMarker) marker.getTag();
            ((TextView)v.findViewById(R.id.info_window_type)).setText("Tipo: " + infoMarker.getType());
            ((TextView)v.findViewById(R.id.info_window_level)).setText("Nivel:  " + infoMarker.getLevel());
            ((TextView)v.findViewById(R.id.info_window_description)).setText("Descripción: " + infoMarker.getDescription());
            ((TextView)v.findViewById(R.id.info_window_date)).setText("Fecha: " + infoMarker.getDate().toString());
            ((TextView)v.findViewById(R.id.info_window_author)).setText("Creado por: " + infoMarker.getAuthor().getUser());

            return v;
        }
    }
}
